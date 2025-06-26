/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalLibraryAbiReader::class)

package org.jetbrains.kotlin.library.abi.parser

import org.jetbrains.kotlin.library.abi.AbiClassifierReference
import org.jetbrains.kotlin.library.abi.AbiQualifiedName
import org.jetbrains.kotlin.library.abi.AbiType
import org.jetbrains.kotlin.library.abi.AbiTypeArgument
import org.jetbrains.kotlin.library.abi.AbiTypeNullability
import org.jetbrains.kotlin.library.abi.AbiVariance
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader

// Convenience extensions for accessing properties that may exist without have to cast repeatedly
// For sources with documentation see
// https://github.com/JetBrains/kotlin/blob/master/compiler/util-klib-abi/src/org/jetbrains/kotlin/library/abi/LibraryAbi.kt

/** A classifier reference is either a simple class or a type reference */
internal val AbiType.classifierReference: AbiClassifierReference?
    get() = (this as? AbiType.Simple)?.classifierReference
/** The class name from a regular type e.g. 'Array' */
internal val AbiType.className: AbiQualifiedName?
    get() = classifierReference?.className
/** A tag from a type type parameter reference e.g. 'T' */
internal val AbiType.tag: String?
    get() = classifierReference?.tag
/** The string representation of a type, whether it is a simple type or a type reference */
internal val AbiType.classNameOrTag: String?
    get() = className?.toString() ?: tag
internal val AbiType.nullability: AbiTypeNullability?
    get() = (this as? AbiType.Simple)?.nullability
internal val AbiType.arguments: List<AbiTypeArgument>?
    get() = (this as? AbiType.Simple)?.arguments
internal val AbiTypeArgument.type: AbiType?
    get() = (this as? AbiTypeArgument.TypeProjection)?.type
internal val AbiTypeArgument.variance: AbiVariance?
    get() = (this as? AbiTypeArgument.TypeProjection)?.variance
internal val AbiClassifierReference.className: AbiQualifiedName?
    get() = (this as? AbiClassifierReference.ClassReference)?.className
internal val AbiClassifierReference.tag: String?
    get() = (this as? AbiClassifierReference.TypeParameterReference)?.tag
