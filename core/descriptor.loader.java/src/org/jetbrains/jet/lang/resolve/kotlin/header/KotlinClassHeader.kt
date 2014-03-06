/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.kotlin.header

import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KotlinSyntheticClass

public class KotlinClassHeader(
        public val kind: KotlinClassHeader.Kind,
        public val version: Int,
        public val annotationData: Array<String>?,
        public val syntheticClassKind: KotlinSyntheticClass.Kind?
) {
    {
        assert((annotationData == null) == (kind != Kind.CLASS && kind != Kind.PACKAGE_FACADE)) {
            "Annotation data should be not null only for CLASS and PACKAGE_FACADE (kind=" + kind + ")"
        }
        assert((syntheticClassKind == null) == (kind != Kind.SYNTHETIC_CLASS)) {
            "Synthetic class kind should be present for SYNTHETIC_CLASS (kind=" + kind + ")"
        }
    }

    public enum class Kind {
        INCOMPATIBLE_ABI_VERSION
        CLASS
        PACKAGE_FACADE
        SYNTHETIC_CLASS
    }
}