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
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode;
import org.jetbrains.jet.codegen.StackValue;

import java.util.Collection;
import java.util.Map;

public class RegeneratedLambdaFieldRemapper extends FieldRemapper {

    private final String oldOwnerType;

    private final String newOwnerType;

    private final Parameters parameters;

    private final Map<String, LambdaInfo> recapturedLambdas;

    public RegeneratedLambdaFieldRemapper(
            String oldOwnerType,
            String newOwnerType,
            Parameters parameters,
            Map<String, LambdaInfo> recapturedLambdas,
            FieldRemapper remapper
    ) {
        super(oldOwnerType, remapper, parameters);
        this.oldOwnerType = oldOwnerType;
        this.newOwnerType = newOwnerType;
        this.parameters = parameters;
        this.recapturedLambdas = recapturedLambdas;
    }

    @Override
    public boolean canProcess(@NotNull String fieldOwner, String fieldName, boolean isFolding) {
        return super.canProcess(fieldOwner, fieldName, isFolding) || isRecapturedLambdaType(fieldOwner);
    }

    private boolean isRecapturedLambdaType(String owner) {
        return recapturedLambdas.containsKey(owner);
    }

    @Nullable
    @Override
    public CapturedParamInfo findField(@NotNull FieldInsnNode fieldInsnNode, @NotNull Collection<CapturedParamInfo> captured) {
        boolean searchInParent = !canProcess(fieldInsnNode.owner, fieldInsnNode.name, false);
        if (searchInParent) {
            return parent.findField(fieldInsnNode);
        } else {
            return findFieldInMyCaptured(fieldInsnNode);
        }
    }

    @Nullable
    public CapturedParamInfo findFieldInMyCaptured(@NotNull FieldInsnNode fieldInsnNode) {
        return super.findField(fieldInsnNode, parameters.getCaptured());
    }

    @Nullable
    @Override
    public StackValue getFieldForInline(@NotNull FieldInsnNode node, @Nullable StackValue prefix) {
        assert node.name.startsWith("$$$") : "Captured field template should start with $$$ prefix";
        FieldInsnNode fin = new FieldInsnNode(node.getOpcode(), node.owner, node.name.substring(3), node.desc);
        CapturedParamInfo field = findFieldInMyCaptured(fin);

        boolean searchInParent = false;
        if (field == null) {
            field = findFieldInMyCaptured(new FieldInsnNode(Opcodes.GETSTATIC, oldOwnerType, "this$0", Type.getObjectType(parent.getLambdaInternalName()).getDescriptor()));
            searchInParent = true;
            if (field == null) {
                throw new IllegalStateException("Couldn't find captured this " + getLambdaInternalName() + " for " + node.name);
            }
        }

        StackValue result = StackValue.field(field.getType(),
                                             Type.getObjectType(newOwnerType), /*TODO owner type*/
                                             field.getNewFieldName(), false,
                                             prefix == null ? StackValue.thiz() : prefix);

        return searchInParent ? parent.getFieldForInline(node, result) : result;
    }
}
