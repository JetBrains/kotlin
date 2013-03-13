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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterSignature;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class ConstructorFrameMap extends FrameMap {
    private int myOuterThisIndex = -1;

    public ConstructorFrameMap(CallableMethod callableMethod, @Nullable ConstructorDescriptor descriptor) {
        enterTemp(OBJECT_TYPE); // this

        List<JvmMethodParameterSignature> parameterTypes = callableMethod.getSignature().getKotlinParameterTypes();
        if (parameterTypes != null) {
            for (JvmMethodParameterSignature parameterType : parameterTypes) {
                if (parameterType.getKind() == JvmMethodParameterKind.OUTER) {
                    myOuterThisIndex = enterTemp(OBJECT_TYPE); // this0
                }
                else if (parameterType.getKind() != JvmMethodParameterKind.VALUE) {
                    enterTemp(parameterType.getAsmType());
                }
                else {
                    break;
                }
            }
        }

        List<Type> explicitArgTypes = callableMethod.getValueParameterTypes();
        List<ValueParameterDescriptor> paramDescrs = descriptor != null
                                                     ? descriptor.getValueParameters()
                                                     : Collections.<ValueParameterDescriptor>emptyList();
        for (int i = 0; i < paramDescrs.size(); i++) {
            ValueParameterDescriptor parameter = paramDescrs.get(i);
            enter(parameter, explicitArgTypes.get(i));
        }
    }

    public int getOuterThisIndex() {
        return myOuterThisIndex;
    }
}
