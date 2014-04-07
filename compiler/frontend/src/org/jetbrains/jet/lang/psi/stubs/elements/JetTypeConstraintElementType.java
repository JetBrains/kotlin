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

package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetTypeConstraint;
import org.jetbrains.jet.lang.psi.stubs.PsiJetTypeConstraintStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetTypeConstraintImpl;

import java.io.IOException;

public class JetTypeConstraintElementType extends JetStubElementType<PsiJetTypeConstraintStub, JetTypeConstraint> {
    public JetTypeConstraintElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetTypeConstraint.class, PsiJetTypeConstraintStub.class);
    }

    @Override
    public PsiJetTypeConstraintStub createStub(@NotNull JetTypeConstraint psi, StubElement parentStub) {
        return new PsiJetTypeConstraintImpl(parentStub, psi.isClassObjectConstraint());
    }

    @Override
    public void serialize(@NotNull PsiJetTypeConstraintStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.isClassObjectConstraint());
    }

    @NotNull
    @Override
    public PsiJetTypeConstraintStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        boolean isClassObjectConstraint = dataStream.readBoolean();
        return new PsiJetTypeConstraintImpl(parentStub, isClassObjectConstraint);
    }
}
