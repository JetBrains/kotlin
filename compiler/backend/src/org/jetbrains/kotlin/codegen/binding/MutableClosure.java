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
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.getDirectMember;

public final class MutableClosure implements CalculatedClosure {
    private final ClassDescriptor closureClass;
    private final ClassDescriptor enclosingClass;
    private final CallableDescriptor enclosingFunWithReceiverDescriptor;

    private boolean captureThis;
    private boolean captureEnclosingReceiver;

    private Map<DeclarationDescriptor, EnclosedValueDescriptor> captureVariables;
    private Map<DeclarationDescriptor, Integer> parameterOffsetInConstructor;
    private List<Pair<String, Type>> recordedFields;
    private KotlinType captureReceiverType;
    private boolean isSuspend;
    private boolean isSuspendLambda;

    MutableClosure(@NotNull ClassDescriptor classDescriptor, @Nullable ClassDescriptor enclosingClass) {
        this.closureClass = classDescriptor;
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

    @NotNull
    @Override
    public ClassDescriptor getClosureClass() {
        return closureClass;
    }

    @Nullable
    public ClassDescriptor getEnclosingClass() {
        return enclosingClass;
    }

    @Override
    public ClassDescriptor getCapturedOuterClassDescriptor() {
        return captureThis ? enclosingClass : null;
    }

    public void setNeedsCaptureOuterClass() {
        this.captureThis = true;
    }

    @Override
    public KotlinType getCapturedReceiverFromOuterContext() {
        if (captureReceiverType != null) {
            return captureReceiverType;
        }

        if (captureEnclosingReceiver) {
            ReceiverParameterDescriptor parameter = getEnclosingReceiverDescriptor();
            assert parameter != null : "Receiver parameter should exist in " + enclosingFunWithReceiverDescriptor;
            return parameter.getType();
        }

        return null;
    }

    @NotNull
    @Override
    public String getCapturedReceiverFieldName(BindingContext bindingContext, LanguageVersionSettings languageVersionSettings) {
        if (captureReceiverType != null) {
            // Should effectively be returned only for callable references
            return AsmUtil.CAPTURED_RECEIVER_FIELD;
        } else if (enclosingFunWithReceiverDescriptor != null) {
            if (!languageVersionSettings.supportsFeature(LanguageFeature.NewCapturedReceiverFieldNamingConvention)) {
                return AsmUtil.CAPTURED_RECEIVER_FIELD;
            }

            String labeledThis = AsmUtil.getLabeledThisNameForReceiver(
                    enclosingFunWithReceiverDescriptor, bindingContext, languageVersionSettings);

            return AsmUtil.getCapturedFieldName(labeledThis);
        } else {
            throw new IllegalStateException("Closure does not capture an outer receiver");
        }
    }

    public void setNeedsCaptureReceiverFromOuterContext() {
        if (enclosingFunWithReceiverDescriptor == null) {
            throw new IllegalStateException("Extension receiver parameter should exist");
        }
        this.captureEnclosingReceiver = true;
    }

    public void setCustomCapturedReceiverType(@NotNull KotlinType type) {
        this.captureReceiverType = type;
    }

    @NotNull
    @Override
    public Map<DeclarationDescriptor, EnclosedValueDescriptor> getCaptureVariables() {
        return captureVariables != null ? captureVariables : Collections.emptyMap();
    }

    @NotNull
    @Override
    public List<Pair<String, Type>> getRecordedFields() {
        return recordedFields != null ? recordedFields : Collections.emptyList();
    }

    @Override
    public boolean isSuspend() {
        return isSuspend;
    }

    public void setSuspend(boolean suspend) {
        this.isSuspend = suspend;
    }

    @Override
    public boolean isSuspendLambda() {
        return isSuspendLambda;
    }

    public void setSuspendLambda() {
        isSuspendLambda = true;
    }

    private void recordField(String name, Type type) {
        if (recordedFields == null) {
            recordedFields = new LinkedList<>();
        }
        recordedFields.add(new Pair<>(name, type));
    }

    public void captureVariable(EnclosedValueDescriptor value) {
        recordField(value.getFieldName(), value.getType());

        if (captureVariables == null) {
            captureVariables = new LinkedHashMap<>();
        }
        captureVariables.put(value.getDescriptor(), value);
    }

    public void setCapturedParameterOffsetInConstructor(DeclarationDescriptor descriptor, int offset) {
        if (parameterOffsetInConstructor == null) {
            parameterOffsetInConstructor = new LinkedHashMap<>();
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
