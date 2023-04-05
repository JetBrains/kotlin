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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.psi.KtProjectionKind;
import org.jetbrains.kotlin.psi.KtUserType;
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub;
import org.jetbrains.kotlin.psi.stubs.StubUtils;
import org.jetbrains.kotlin.psi.stubs.impl.Argument;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFlexibleType;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinUserTypeStubImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KtUserTypeElementType extends KtStubElementType<KotlinUserTypeStub, KtUserType> {
    public KtUserTypeElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtUserType.class, KotlinUserTypeStub.class);
    }

    @NotNull
    @Override
    public KotlinUserTypeStub createStub(@NotNull KtUserType psi, StubElement parentStub) {
        return new KotlinUserTypeStubImpl((StubElement<?>) parentStub, null);
    }

    @Override
    public void serialize(@NotNull KotlinUserTypeStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        serializeType(dataStream, ((KotlinUserTypeStubImpl) stub).getUpperBound());
    }

    private static void serializeType(@NotNull StubOutputStream dataStream, @Nullable KotlinFlexibleType type) throws IOException {
        dataStream.writeBoolean(type != null);
        if (type != null) {
            StubUtils.serializeClassId(dataStream, type.getClassId());
            dataStream.writeBoolean(type.getNullable());
            List<Argument> arguments = type.getArguments();
            dataStream.writeInt(arguments.size());
            for (Argument argument : arguments) {
                KtProjectionKind kind = argument.getProjectionKind();
                dataStream.writeInt(kind.ordinal());
                if (kind != KtProjectionKind.STAR) {
                    serializeType(dataStream, argument.getType());
                }
            }
            serializeType(dataStream, type.getUpperBound());
        }
    }

    @NotNull
    @Override
    public KotlinUserTypeStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new KotlinUserTypeStubImpl((StubElement<?>) parentStub, deserializeType(dataStream));
    }

    @Nullable
    private static KotlinFlexibleType deserializeType(@NotNull StubInputStream dataStream) throws IOException {
        boolean hasFlexibleType = dataStream.readBoolean();
        if (hasFlexibleType) {
            ClassId classId = Objects.requireNonNull(StubUtils.deserializeClassId(dataStream));
            boolean isNullable = dataStream.readBoolean();
            int count = dataStream.readInt();
            List<Argument> arguments = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int kind = dataStream.readInt();
                Argument argument;
                if (kind != KtProjectionKind.STAR.ordinal()) {
                    argument = new Argument(KtProjectionKind.values()[kind], deserializeType(dataStream));
                }
                else {
                    argument = new Argument(KtProjectionKind.STAR, null);
                }
                arguments.add(argument);
            }
            return new KotlinFlexibleType(classId, arguments, isNullable, deserializeType(dataStream));
        }
        return null;
    }
}
