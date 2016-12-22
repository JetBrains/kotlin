/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@file:JvmName("KClassifiers")
package kotlin.reflect

import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType

/**
 * Creates a [KType] instance with the given classifier, type arguments, nullability and annotations.
 * If the number of passed type arguments is not equal to the total number of type parameters of a classifier,
 * an exception is thrown. If any of the arguments does not satisfy the bounds of the corresponding type parameter,
 * an exception is thrown.
 *
 * For classifiers representing type parameters, the type argument list must always be empty.
 * For classes, the type argument list should contain arguments for the type parameters of the class. If the class is `inner`,
 * the list should follow with arguments for the type parameters of its outer class, and so forth until a class is
 * not `inner`, or is declared on the top level.
 */
@SinceKotlin("1.1")
@Deprecated("Use 'createType' from kotlin.reflect.full package", ReplaceWith("this.createType(arguments, nullable, annotations)", "kotlin.reflect.full.createType"), level = DeprecationLevel.ERROR)
inline fun KClassifier.createType(
        arguments: List<KTypeProjection> = emptyList(),
        nullable: Boolean = false,
        annotations: List<Annotation> = emptyList()
): KType = this.createType(arguments, nullable, annotations)

/**
 * Creates an instance of [KType] with the given classifier, substituting all its type parameters with star projections.
 * The resulting type is not marked as nullable and does not have any annotations.
 *
 * @see [KClassifier.createType]
 */
@SinceKotlin("1.1")
@Deprecated("Use 'starProjectedType' from kotlin.reflect.full package", ReplaceWith("this.starProjectedType", "kotlin.reflect.full.starProjectedType"), level = DeprecationLevel.ERROR)
inline val KClassifier.starProjectedType: KType
    get() = this.starProjectedType