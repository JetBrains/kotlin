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
import org.jetbrains.kotlin.psi.KtBackingField;
import org.jetbrains.kotlin.psi.stubs.KotlinBackingFieldStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinBackingFieldStubImpl;

import java.io.IOException;

public class KtBackingFieldElementType extends KtStubElementType<KotlinBackingFieldStub, KtBackingField> {
    public KtBackingFieldElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtBackingField.class, KotlinBackingFieldStub.class);
    }

    @Override
    public KotlinBackingFieldStub createStub(@NotNull KtBackingField psi, StubElement parentStub) {
        return new KotlinBackingFieldStubImpl(parentStub, psi.hasInitializer());
    }

    @Override
    public void serialize(@NotNull KotlinBackingFieldStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.hasInitializer());
    }

    @NotNull
    @Override
    public KotlinBackingFieldStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        boolean hasInitializer = dataStream.readBoolean();
        return new KotlinBackingFieldStubImpl(parentStub, hasInitializer);
    }
}
