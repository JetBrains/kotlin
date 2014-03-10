/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.tree.FieldInsnNode;
import org.jetbrains.jet.codegen.StackValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RegeneratedLambdaFieldRemapper extends LambdaFieldRemapper {

    private final String oldOwnerType;

    private final String newOwnerType;

    private final Parameters parameters;

    private final Map<String, LambdaInfo> recapturedLambdas;

    public RegeneratedLambdaFieldRemapper(
            String oldOwnerType,
            String newOwnerType,
            Parameters parameters,
            Map<String, LambdaInfo> recapturedLambdas,
            LambdaFieldRemapper remapper
    ) {
        super(oldOwnerType, remapper, parameters);
        this.oldOwnerType = oldOwnerType;
        this.newOwnerType = newOwnerType;
        this.parameters = parameters;
        this.recapturedLambdas = recapturedLambdas;
    }

    @Override
    public void addCapturedFields(LambdaInfo lambdaInfo, ParametersBuilder builder) {
        if (canProcess(lambdaInfo.getLambdaClassType().getInternalName())) {
            List<CapturedParamInfo> captured = parameters.getCaptured();
            for (CapturedParamInfo originalField : lambdaInfo.getCapturedVars()) {
                CapturedParamInfo foundField = null;
                for (CapturedParamInfo capturedParamInfo : captured) {
                    if (capturedParamInfo.getContainingLambdaName().equals(originalField.getContainingLambdaName())) {
                        if (capturedParamInfo.getFieldName().equals(LambdaTransformer.getNewFieldName(originalField.getFieldName()))) {
                            foundField = originalField;
                            break;
                        }
                    }
                }

                if (foundField == null) {
                    throw new IllegalStateException("Captured parameter should exists in outer context: " + originalField.getFieldName());
                }

                CapturedParamInfo info = builder.addCapturedParam(foundField, foundField);
            }
        } else {
            //in case when inlining lambda into another one inside inline function
            parent.addCapturedFields(lambdaInfo, builder);
        }
    }

    @Override
    public boolean canProcess(@NotNull String fieldOwner) {
        return super.canProcess(fieldOwner) || isRecapturedLambdaType(fieldOwner);
    }

    private boolean isRecapturedLambdaType(String owner) {
        return recapturedLambdas.containsKey(owner);
    }

    @Nullable
    @Override
    public CapturedParamInfo findField(@NotNull FieldInsnNode fieldInsnNode, @NotNull Collection<CapturedParamInfo> captured) {
        boolean searchInParent = !canProcess(fieldInsnNode.owner);
        if (searchInParent) {
            return parent.findField(fieldInsnNode);
        } else if (isRecapturedLambdaType(fieldInsnNode.owner)) {
            LambdaInfo info = recapturedLambdas.get(fieldInsnNode.owner);
            return super.findField(fieldInsnNode, info.getCapturedVars());
        }
        else {
            return super.findField(fieldInsnNode, captured);
        }
    }

    @Nullable
    public CapturedParamInfo findFieldInMyCaptured(@NotNull FieldInsnNode fieldInsnNode) {
        if (isRecapturedLambdaType(fieldInsnNode.owner)) {
            LambdaInfo info = recapturedLambdas.get(fieldInsnNode.owner);
            return super.findField(fieldInsnNode, info.getCapturedVars());
        }
        else {
            return super.findField(fieldInsnNode, parameters.getCaptured());
        }
    }

    @Nullable
    @Override
    public StackValue getFieldForInline(@NotNull FieldInsnNode node, @Nullable StackValue prefix) {
        FieldInsnNode fin = new FieldInsnNode(node.getOpcode(), node.owner, node.name.substring(3), node.desc);
        CapturedParamInfo field = findFieldInMyCaptured(fin);

        boolean searchInParent = false;
        if (field == null) {
            field = findFieldInMyCaptured(new FieldInsnNode(Opcodes.GETSTATIC, oldOwnerType, "this$0", Type.getObjectType(parent.getLambdaInternalName()).getDescriptor()));
            searchInParent = true;
            if (field == null) {
                throw new IllegalStateException("Could find captured this " + getLambdaInternalName());
            }
        }

        String newName = field.getContainingLambdaName().equals(getLambdaInternalName())
                         ? field.getFieldName()
                         : LambdaTransformer.getNewFieldName(field.getFieldName());
        StackValue result =
                StackValue.composed(prefix == null ? StackValue.local(0, Type.getObjectType(getLambdaInternalName())) : prefix,
                                    StackValue.field(field.getType(),
                                                     Type.getObjectType(newOwnerType), /*TODO owner type*/
                                                     newName, false)
        );

        return searchInParent ? parent.getFieldForInline(node, result) : result;
    }
}
