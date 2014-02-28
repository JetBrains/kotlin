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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KotlinClassHeader {

    public enum Kind {
        CLASS,
        PACKAGE_FACADE,
        SYNTHETIC_CLASS,
        INCOMPATIBLE_ABI_VERSION
    }

    private final Kind kind;
    private final int version;
    private final String[] data;

    public KotlinClassHeader(@NotNull Kind kind, int version, @Nullable String[] annotationData) {
        assert (annotationData == null) == (kind != Kind.CLASS && kind != Kind.PACKAGE_FACADE)
                : "Annotation data should be not null only for CLASS and PACKAGE_FACADE";
        this.kind = kind;
        this.version = version;
        this.data = annotationData;
    }

    @NotNull
    public Kind getKind() {
        return kind;
    }

    public int getVersion() {
        return version;
    }

    @Nullable
    public String[] getAnnotationData() {
        return data;
    }
}
