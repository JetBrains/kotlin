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

package org.jetbrains.kotlin.descriptors.annotations

import org.jetbrains.kotlin.name.FqName

val ADDITIONAL_ANNOTATIONS_MAP: Map<FqName, String> = mapOf(
        FqName("kotlin.tailRecursive") to "tailrec",
        FqName("kotlin.jvm.native") to "external",
        FqName("kotlin.inlineOptions") to "crossinline"
)

// Please synchronize this set with JetTokens.ANNOTATION_MODIFIERS_KEYWORDS_ARRAY
public val ANNOTATION_MODIFIERS_FQ_NAMES: Set<FqName> =
        arrayOf("data", "inline", "noinline", "tailrec", "external", "annotation.annotation", "crossinline").map { FqName("kotlin.$it") }.toSet()

public val ANNOTATIONS_SHOULD_BE_REPLACED_WITH_MODIFIERS_FQ_NAMES: Set<FqName> =
        ANNOTATION_MODIFIERS_FQ_NAMES + ADDITIONAL_ANNOTATIONS_MAP.keySet()


public val ANNOTATION_MODIFIERS_MAP: Map<FqName, String> =
        ANNOTATION_MODIFIERS_FQ_NAMES.map { it to it.shortName().asString() }.toMap() +
        ADDITIONAL_ANNOTATIONS_MAP
