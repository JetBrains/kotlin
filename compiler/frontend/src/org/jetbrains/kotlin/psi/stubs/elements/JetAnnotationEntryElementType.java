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

import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.JetAnnotationEntry;
import org.jetbrains.kotlin.psi.JetPsiUtil;
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationEntryStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationEntryStubImpl;
import org.jetbrains.kotlin.name.Name;

import java.io.IOException;

public class JetAnnotationEntryElementType extends JetStubElementType<KotlinAnnotationEntryStub, JetAnnotationEntry> {

    public JetAnnotationEntryElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetAnnotationEntry.class, KotlinAnnotationEntryStub.class);
    }

    @Override
    public KotlinAnnotationEntryStub createStub(@NotNull JetAnnotationEntry psi, StubElement parentStub) {
        Name shortName = JetPsiUtil.getShortName(psi);
        String resultName = shortName != null ? shortName.asString() : psi.getText();
        boolean hasValueArguments = psi.getValueArgumentList() != null;
        return new KotlinAnnotationEntryStubImpl(parentStub, StringRef.fromString(resultName), hasValueArguments);
    }

    @Override
    public void serialize(@NotNull KotlinAnnotationEntryStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getShortName());
        dataStream.writeBoolean(stub.hasValueArguments());
    }

    @NotNull
    @Override
    public KotlinAnnotationEntryStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef text = dataStream.readName();
        boolean hasValueArguments = dataStream.readBoolean();
        return new KotlinAnnotationEntryStubImpl(parentStub, text, hasValueArguments);
    }

    @Override
    public void indexStub(@NotNull KotlinAnnotationEntryStub stub, @NotNull IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexAnnotation(stub, sink);
    }
}
