/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi.stubs.impl;

import com.intellij.psi.stubs.StubElement;
import org.jetbrains.jet.lang.psi.JetTypeConstraint;
import org.jetbrains.jet.lang.psi.stubs.PsiJetTypeConstraintStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;

public class PsiJetTypeConstraintImpl extends JetStubBaseImpl<JetTypeConstraint> implements PsiJetTypeConstraintStub {
    private final boolean isClassObjectConstraint;

    public PsiJetTypeConstraintImpl(StubElement parent, boolean isClassObjectConstraint) {
        super(parent, JetStubElementTypes.TYPE_CONSTRAINT);
        this.isClassObjectConstraint = isClassObjectConstraint;
    }

    @Override
    public boolean isClassObjectConstraint() {
        return isClassObjectConstraint;
    }
}
