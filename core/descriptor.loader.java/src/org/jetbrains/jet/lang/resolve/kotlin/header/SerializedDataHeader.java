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

package org.jetbrains.jet.lang.resolve.kotlin.header;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.FqName;

import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;

public class SerializedDataHeader extends KotlinClassFileHeader {
    private static final Logger LOG = Logger.getInstance(SerializedDataHeader.class);

    public enum Kind {
        CLASS,
        PACKAGE
    }

    private final String[] data;
    private final Kind kind;

    private SerializedDataHeader(int version, @Nullable String[] annotationData, @NotNull Kind kind, @NotNull FqName fqName) {
        super(version, fqName);
        this.data = annotationData;
        this.kind = kind;
    }

    @Nullable
    public static SerializedDataHeader create(int version, @Nullable String[] annotationData, @NotNull Kind kind, @NotNull FqName fqName) {
        if (isAbiVersionCompatible(version) && annotationData == null) {
            LOG.error("Kotlin annotation " + kind + " is incorrect for class: " + fqName);
            return null;
        }
        return new SerializedDataHeader(version, annotationData, kind, fqName);
    }

    @Nullable
    public String[] getAnnotationData() {
        return data;
    }

    @NotNull
    public Kind getKind() {
        return kind;
    }
}
