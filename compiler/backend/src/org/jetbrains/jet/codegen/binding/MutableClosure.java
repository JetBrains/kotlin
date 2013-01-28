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

package org.jetbrains.jet.codegen.binding;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperCall;

import java.util.*;

public final class MutableClosure implements CalculatedClosure {
    private final JetDelegatorToSuperCall superCall;

    private final ClassDescriptor enclosingClass;
    private final CallableDescriptor enclosingReceiverDescriptor;

    private boolean captureThis;
    private boolean captureReceiver;

    private Map<DeclarationDescriptor, EnclosedValueDescriptor> captureVariables;
    private List<Pair<String, Type>> recordedFields;

    MutableClosure(
            JetDelegatorToSuperCall superCall,
            ClassDescriptor enclosingClass,
            CallableDescriptor enclosingReceiverDescriptor
    ) {
        this.superCall = superCall;
        this.enclosingClass = enclosingClass;
        this.enclosingReceiverDescriptor = enclosingReceiverDescriptor;
    }

    @Nullable
    @Override
    public ClassDescriptor getEnclosingClass() {
        return enclosingClass;
    }

    @Override
    public JetDelegatorToSuperCall getSuperCall() {
        return superCall;
    }

    @Override
    public ClassDescriptor getCaptureThis() {
        return captureThis ? enclosingClass : null;
    }

    public void setCaptureThis() {
        this.captureThis = true;
    }

    @Override
    public ClassifierDescriptor getCaptureReceiver() {
        return captureReceiver
               ? enclosingReceiverDescriptor.getReceiverParameter().getType().getConstructor().getDeclarationDescriptor()
               : null;
    }

    public void setCaptureReceiver() {
        if (enclosingReceiverDescriptor == null) {
            throw new IllegalStateException();
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
            captureVariables = new HashMap<DeclarationDescriptor, EnclosedValueDescriptor>();
        }
        captureVariables.put(value.getDescriptor(), value);
    }

    public CallableDescriptor getEnclosingReceiverDescriptor() {
        return enclosingReceiverDescriptor;
    }
}
