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
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.psi.KtProjectionKind;
import org.jetbrains.kotlin.psi.KtUserType;
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub;
import org.jetbrains.kotlin.psi.stubs.StubUtils;
import org.jetbrains.kotlin.psi.stubs.impl.*;

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

    private enum KotlinTypeBeanKind {
        CLASS, TYPE_PARAMETER, FLEXIBLE, NONE;

        static KotlinTypeBeanKind fromBean(@Nullable KotlinTypeBean typeBean) {
            if (typeBean == null) return NONE;
            if (typeBean instanceof KotlinTypeParameterTypeBean) return TYPE_PARAMETER;
            if (typeBean instanceof KotlinClassTypeBean) return CLASS;
            return FLEXIBLE;
        }
    }

    private static void serializeType(@NotNull StubOutputStream dataStream, @Nullable KotlinTypeBean type) throws IOException {
        dataStream.writeInt(KotlinTypeBeanKind.fromBean(type).ordinal());
        if (type instanceof KotlinClassTypeBean) {
            StubUtils.serializeClassId(dataStream, ((KotlinClassTypeBean) type).getClassId());
            dataStream.writeBoolean(type.getNullable());
            List<KotlinTypeArgumentBean> arguments = ((KotlinClassTypeBean) type).getArguments();
            dataStream.writeInt(arguments.size());
            for (KotlinTypeArgumentBean argument : arguments) {
                KtProjectionKind kind = argument.getProjectionKind();
                dataStream.writeInt(kind.ordinal());
                if (kind != KtProjectionKind.STAR) {
                    serializeType(dataStream, argument.getType());
                }
            }
        }
        else if (type instanceof KotlinTypeParameterTypeBean) {
            dataStream.writeName(((KotlinTypeParameterTypeBean) type).getTypeParameterName());
            dataStream.writeBoolean(type.getNullable());
            dataStream.writeBoolean(((KotlinTypeParameterTypeBean) type).getDefinitelyNotNull());
        }
        else if (type instanceof KotlinFlexibleTypeBean) {
            serializeType(dataStream, ((KotlinFlexibleTypeBean) type).getLowerBound());
            serializeType(dataStream, ((KotlinFlexibleTypeBean) type).getUpperBound());
        }
    }

    @NotNull
    @Override
    public KotlinUserTypeStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new KotlinUserTypeStubImpl((StubElement<?>) parentStub, deserializeType(dataStream));
    }

    @Nullable
    private static KotlinTypeBean deserializeType(@NotNull StubInputStream dataStream) throws IOException {
        KotlinTypeBeanKind typeKind = KotlinTypeBeanKind.values()[dataStream.readInt()];
        switch (typeKind) {
            case CLASS: {
                ClassId classId = Objects.requireNonNull(StubUtils.deserializeClassId(dataStream));
                boolean isNullable = dataStream.readBoolean();
                int count = dataStream.readInt();
                List<KotlinTypeArgumentBean> arguments = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    int kind = dataStream.readInt();
                    KotlinTypeArgumentBean argument;
                    if (kind != KtProjectionKind.STAR.ordinal()) {
                        argument = new KotlinTypeArgumentBean(KtProjectionKind.values()[kind], deserializeType(dataStream));
                    }
                    else {
                        argument = new KotlinTypeArgumentBean(KtProjectionKind.STAR, null);
                    }
                    arguments.add(argument);
                }
                return new KotlinClassTypeBean(classId, arguments, isNullable);
            }
            case TYPE_PARAMETER: {
                String typeParameterName = Objects.requireNonNull(dataStream.readNameString());
                boolean nullable = dataStream.readBoolean();
                boolean definitelyNotNull = dataStream.readBoolean();
                return new KotlinTypeParameterTypeBean(typeParameterName, nullable, definitelyNotNull);
            }
            case FLEXIBLE: {
                return new KotlinFlexibleTypeBean(Objects.requireNonNull(deserializeType(dataStream)),
                                                  Objects.requireNonNull(deserializeType(dataStream)));
            }
            case NONE:
                return null;
        }
        return null;
    }
}
