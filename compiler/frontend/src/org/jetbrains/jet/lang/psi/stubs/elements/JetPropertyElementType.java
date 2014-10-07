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

package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.stubs.PsiJetPropertyStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetPropertyStubImpl;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;

public class JetPropertyElementType extends JetStubElementType<PsiJetPropertyStub, JetProperty> {
    public JetPropertyElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetProperty.class, PsiJetPropertyStub.class);
    }

    @Override
    public PsiJetPropertyStub createStub(@NotNull JetProperty psi, StubElement parentStub) {
        assert !psi.isLocal() :
                String.format("Should not store local property: %s, parent %s",
                              psi.getText(), psi.getParent() != null ? psi.getParent().getText() : "<no parent>");

        return new PsiJetPropertyStubImpl(
                parentStub, StringRef.fromString(psi.getName()),
                psi.isVar(), psi.isTopLevel(), psi.hasDelegate(),
                psi.hasDelegateExpression(), psi.hasInitializer(),
                psi.getReceiverTypeReference() != null, psi.getTypeReference() != null,
                ResolveSessionUtils.safeFqNameForLazyResolve(psi)
        );
    }

    @Override
    public void serialize(@NotNull PsiJetPropertyStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isVar());
        dataStream.writeBoolean(stub.isTopLevel());
        dataStream.writeBoolean(stub.hasDelegate());
        dataStream.writeBoolean(stub.hasDelegateExpression());
        dataStream.writeBoolean(stub.hasInitializer());
        dataStream.writeBoolean(stub.hasReceiverTypeRef());
        dataStream.writeBoolean(stub.hasReturnTypeRef());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.asString() : null);
    }

    @NotNull
    @Override
    public PsiJetPropertyStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isVar = dataStream.readBoolean();
        boolean isTopLevel = dataStream.readBoolean();
        boolean hasDelegate = dataStream.readBoolean();
        boolean hasDelegateExpression = dataStream.readBoolean();
        boolean hasInitializer = dataStream.readBoolean();
        boolean hasReceiverTypeRef = dataStream.readBoolean();
        boolean hasReturnTypeRef = dataStream.readBoolean();

        StringRef fqNameAsString = dataStream.readName();
        FqName fqName = fqNameAsString != null ? new FqName(fqNameAsString.toString()) : null;

        return new PsiJetPropertyStubImpl(parentStub, name, isVar, isTopLevel, hasDelegate,
                                          hasDelegateExpression, hasInitializer, hasReceiverTypeRef, hasReturnTypeRef, fqName);
    }

    @Override
    public void indexStub(@NotNull PsiJetPropertyStub stub, @NotNull IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexProperty(stub, sink);
    }
}
