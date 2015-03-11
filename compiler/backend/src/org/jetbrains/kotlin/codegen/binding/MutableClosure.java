/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.binding;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.getDirectMember;

public final class MutableClosure implements CalculatedClosure {
    private final ClassDescriptor enclosingClass;
    private final CallableDescriptor enclosingFunWithReceiverDescriptor;

    private boolean captureThis;
    private boolean captureReceiver;

    private Map<DeclarationDescriptor, EnclosedValueDescriptor> captureVariables;
    private Map<DeclarationDescriptor, Integer> parameterOffsetInConstructor;
    private List<Pair<String, Type>> recordedFields;

    MutableClosure(@NotNull ClassDescriptor classDescriptor, @Nullable ClassDescriptor enclosingClass) {
        this.enclosingClass = enclosingClass;
        this.enclosingFunWithReceiverDescriptor = enclosingExtensionMemberForClass(classDescriptor);
    }

    @Nullable
    private static CallableDescriptor enclosingExtensionMemberForClass(@NotNull ClassDescriptor classDescriptor) {
        DeclarationDescriptor classContainer = classDescriptor.getContainingDeclaration();
        if (classContainer instanceof CallableMemberDescriptor) {
            CallableMemberDescriptor member = getDirectMember((CallableMemberDescriptor) classContainer);
            if (member.getExtensionReceiverParameter() != null) {
                return member;
            }
        }
        return null;
    }

    @Nullable
    public ClassDescriptor getEnclosingClass() {
        return enclosingClass;
    }

    @Override
    public ClassDescriptor getCaptureThis() {
        return captureThis ? enclosingClass : null;
    }

    public void setCaptureThis() {
        this.captureThis = true;
    }

    @Override
    public JetType getCaptureReceiverType() {
        if (captureReceiver) {
            ReceiverParameterDescriptor parameter = getEnclosingReceiverDescriptor();
            assert parameter != null : "Receiver parameter should exist in " + enclosingFunWithReceiverDescriptor;
            return parameter.getType();
        }

        return null;
    }

    public void setCaptureReceiver() {
        if (enclosingFunWithReceiverDescriptor == null) {
            throw new IllegalStateException("Extension receiver parameter should exist");
        }
        this.captureReceiver = true;
    }

    @NotNull
    @Override
    public Map<DeclarationDescriptor, EnclosedValueDescriptor> getCaptureVariables() {
        return captureVariables != null ? captureVariables : Collections.<DeclarationDescriptor, EnclosedValueDescriptor>emptyMap();
    }

    @NotNull
    @Override
    public List<Pair<String, Type>> getRecordedFields() {
        return recordedFields != null ? recordedFields : Collections.<Pair<String, Type>>emptyList();
    }

    public void recordField(String name, Type type) {
        if (recordedFields == null) {
            recordedFields = new LinkedList<Pair<String, Type>>();
        }
        recordedFields.add(new Pair<String, Type>(name, type));
    }

    public void captureVariable(EnclosedValueDescriptor value) {
        if (captureVariables == null) {
            captureVariables = new LinkedHashMap<DeclarationDescriptor, EnclosedValueDescriptor>();
        }
        captureVariables.put(value.getDescriptor(), value);
    }

    public void setCapturedParameterOffsetInConstructor(DeclarationDescriptor descriptor, int offset) {
        if (parameterOffsetInConstructor == null) {
            parameterOffsetInConstructor = new LinkedHashMap<DeclarationDescriptor, Integer>();
        }
        parameterOffsetInConstructor.put(descriptor, offset);
    }

    public int getCapturedParameterOffsetInConstructor(DeclarationDescriptor descriptor) {
        Integer result = parameterOffsetInConstructor != null ? parameterOffsetInConstructor.get(descriptor) : null;
        return result != null ? result.intValue() : -1;
    }

    @Nullable
    public ReceiverParameterDescriptor getEnclosingReceiverDescriptor() {
        return enclosingFunWithReceiverDescriptor != null ? enclosingFunWithReceiverDescriptor.getExtensionReceiverParameter() : null;
    }
}
