/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.data;

import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.Set;

/**
 * @author svtk
 */
public class DeclarationData {
    public final JetElement element;
    public final PseudocodeData pseudocodeData;
    public final Set<VariableDescriptor> declaredVariables;
    public final Set<VariableDescriptor> usedVariables;

    public DeclarationData(JetElement element,
            PseudocodeData data,
            Set<VariableDescriptor> declaredVariables,
            Set<VariableDescriptor> usedVariables) {
        this.element = element;
        pseudocodeData = data;
        this.declaredVariables = declaredVariables;
        this.usedVariables = usedVariables;
    }
}
