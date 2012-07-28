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

package org.jetbrains.jet.lang.psi.stubs.impl;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.jet.lang.psi.JetParameterList;
import org.jetbrains.jet.lang.psi.stubs.PsiJetParameterListStub;

/**
 * @author Nikolay Krasko
 */
public class PsiJetParameterListStubImpl extends StubBase<JetParameterList> implements PsiJetParameterListStub {
    public PsiJetParameterListStubImpl(IStubElementType elementType, StubElement parent) {
        super(parent, elementType);
    }

    @Override
    public String toString() {
        return "PsiJetParameterListStubImpl";
    }
}
