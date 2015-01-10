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
import org.jetbrains.kotlin.psi.JetUserType;
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinUserTypeStubImpl;

import java.io.IOException;

public class JetUserTypeElementType extends JetStubElementType<KotlinUserTypeStub, JetUserType> {
    public JetUserTypeElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetUserType.class, KotlinUserTypeStub.class);
    }

    @Override
    public KotlinUserTypeStub createStub(@NotNull JetUserType psi, StubElement parentStub) {
        return new KotlinUserTypeStubImpl(parentStub, psi.isAbsoluteInRootPackage());
    }

    @Override
    public void serialize(@NotNull KotlinUserTypeStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.isAbsoluteInRootPackage());
    }

    @NotNull
    @Override
    public KotlinUserTypeStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        boolean isAbsoluteInRootPackage = dataStream.readBoolean();
        return new KotlinUserTypeStubImpl(parentStub, isAbsoluteInRootPackage);
    }
}
