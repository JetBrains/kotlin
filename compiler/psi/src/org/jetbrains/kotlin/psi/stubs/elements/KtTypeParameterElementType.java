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
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtTypeParameter;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeParameterStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeParameterStubImpl;
import org.jetbrains.kotlin.types.Variance;

import java.io.IOException;

public class KtTypeParameterElementType extends KtStubElementType<KotlinTypeParameterStub, KtTypeParameter> {
    public KtTypeParameterElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtTypeParameter.class, KotlinTypeParameterStub.class);
    }

    @NotNull
    @Override
    public KotlinTypeParameterStub createStub(@NotNull KtTypeParameter psi, StubElement parentStub) {
        return new KotlinTypeParameterStubImpl(
                (StubElement<?>) parentStub, StringRef.fromString(psi.getName()),
                psi.getVariance() == Variance.IN_VARIANCE, psi.getVariance() == Variance.OUT_VARIANCE
        );
    }

    @Override
    public void serialize(@NotNull KotlinTypeParameterStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isInVariance());
        dataStream.writeBoolean(stub.isOutVariance());
    }

    @NotNull
    @Override
    public KotlinTypeParameterStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isInVariance = dataStream.readBoolean();
        boolean isOutVariance = dataStream.readBoolean();

        return new KotlinTypeParameterStubImpl((StubElement<?>) parentStub, name, isInVariance, isOutVariance);
    }
}
