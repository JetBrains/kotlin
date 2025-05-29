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
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.stubs.KotlinNameReferenceExpressionStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl;

import java.io.IOException;

public class KtNameReferenceExpressionElementType extends KtStubElementType<KotlinNameReferenceExpressionStub, KtNameReferenceExpression> {
    public KtNameReferenceExpressionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtNameReferenceExpression.class, KotlinNameReferenceExpressionStub.class);
    }

    @Override
    public KotlinNameReferenceExpressionStub createStub(@NotNull KtNameReferenceExpression psi, StubElement parentStub) {
        return new KotlinNameReferenceExpressionStubImpl(parentStub, StringRef.fromString(psi.getReferencedName()));
    }

    @Override
    public void serialize(@NotNull KotlinNameReferenceExpressionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getReferencedName());
        dataStream.writeBoolean(
                stub instanceof KotlinNameReferenceExpressionStubImpl && ((KotlinNameReferenceExpressionStubImpl) stub).isClassRef());
    }

    @NotNull
    @Override
    public KotlinNameReferenceExpressionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef referencedName = dataStream.readName();
        boolean isClassRef = dataStream.readBoolean();
        return new KotlinNameReferenceExpressionStubImpl(parentStub, referencedName, isClassRef);
    }
}
