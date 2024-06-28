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
import org.jetbrains.kotlin.constant.ConstantValue;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtValueArgumentList;
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationEntryStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationEntryStubImpl;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstantValueKt;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class KtAnnotationEntryElementType extends KtStubElementType<KotlinAnnotationEntryStub, KtAnnotationEntry> {

    public KtAnnotationEntryElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtAnnotationEntry.class, KotlinAnnotationEntryStub.class);
    }

    @NotNull
    @Override
    public KotlinAnnotationEntryStub createStub(@NotNull KtAnnotationEntry psi, StubElement parentStub) {
        Name shortName = psi.getShortName();
        String resultName = shortName != null ? shortName.asString() : null;
        KtValueArgumentList valueArgumentList = psi.getValueArgumentList();
        boolean hasValueArguments = valueArgumentList != null && !valueArgumentList.getArguments().isEmpty();
        return new KotlinAnnotationEntryStubImpl((StubElement<?>) parentStub, StringRef.fromString(resultName), hasValueArguments, null);
    }

    @Override
    public void serialize(@NotNull KotlinAnnotationEntryStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getShortName());
        dataStream.writeBoolean(stub.hasValueArguments());
        if (stub instanceof KotlinAnnotationEntryStubImpl) {
            Map<Name, ConstantValue<?>> arguments = ((KotlinAnnotationEntryStubImpl) stub).getValueArguments();
            dataStream.writeInt(arguments != null ? arguments.size() : 0);
            if (arguments != null) {
                for (Map.Entry<Name, ConstantValue<?>> valueEntry : arguments.entrySet()) {
                    dataStream.writeName(valueEntry.getKey().asString());
                    ConstantValue<?> value = valueEntry.getValue();
                    KotlinConstantValueKt.serialize(value, dataStream);
                }
            }
        }
    }

    @NotNull
    @Override
    public KotlinAnnotationEntryStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef text = dataStream.readName();
        boolean hasValueArguments = dataStream.readBoolean();
        int valueArgCount = dataStream.readInt();
        Map<Name, ConstantValue<?>> args = new LinkedHashMap<>();
        for (int i = 0; i < valueArgCount; i++) {
            args.put(Name.identifier(Objects.requireNonNull(dataStream.readNameString())),
                     KotlinConstantValueKt.createConstantValue(dataStream));
        }
        return new KotlinAnnotationEntryStubImpl((StubElement<?>) parentStub, text, hasValueArguments, args.isEmpty() ? null : args);
    }

    @Override
    public void indexStub(@NotNull KotlinAnnotationEntryStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexAnnotation(stub, sink);
    }
}
