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

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.JetTypeConstraint;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeConstraintStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeConstraintStubImpl;

import java.io.IOException;

public class JetTypeConstraintElementType extends JetStubElementType<KotlinTypeConstraintStub, JetTypeConstraint> {
    public JetTypeConstraintElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetTypeConstraint.class, KotlinTypeConstraintStub.class);
    }

    @Override
    public KotlinTypeConstraintStub createStub(@NotNull JetTypeConstraint psi, StubElement parentStub) {
        return new KotlinTypeConstraintStubImpl(parentStub, psi.isDefaultObjectConstraint());
    }

    @Override
    public void serialize(@NotNull KotlinTypeConstraintStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.isDefaultObjectConstraint());
    }

    @NotNull
    @Override
    public KotlinTypeConstraintStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        boolean isDefaultObjectConstraint = dataStream.readBoolean();
        return new KotlinTypeConstraintStubImpl(parentStub, isDefaultObjectConstraint);
    }
}
