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

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.types.model.FlexibleTypeMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeArgumentMarker

/**
 * @param upperBound If [upperBound] exists, this [KotlinUserTypeStubImpl] is a *flexible type* with itself as the lower bound and
 *  [upperBound] as the upper bound.
 * @param abbreviatedType The type alias application from which this type was originally expanded. It can be used to render or navigate to
 *  the original type alias instead of the expanded type.
 */
class KotlinUserTypeStubImpl(
    parent: StubElement<out PsiElement>?,
    val upperBound: KotlinTypeBean? = null,
    val abbreviatedType: KotlinClassTypeBean? = null,
) : KotlinStubBaseImpl<KtUserType>(parent, KtStubElementTypes.USER_TYPE), KotlinUserTypeStub

sealed interface KotlinTypeBean : KotlinTypeMarker {
    val nullable: Boolean
}

data class KotlinFlexibleTypeBean(val lowerBound: KotlinTypeBean, val upperBound: KotlinTypeBean) : KotlinTypeBean, FlexibleTypeMarker {
    override val nullable: Boolean
        get() = lowerBound.nullable
}

/**
 * If this [KotlinClassTypeBean] is an expanded type, [abbreviatedType] stores its abbreviated type (an application of a type alias),
 * similar to [KotlinUserTypeStubImpl.abbreviatedType]. The abbreviated type may itself contain type arguments, which are represented as
 * class type beans of *expanded types*. This allows resolving the expanded type instead of the abbreviated type if any type alias contained
 * in the type is not resolvable from a given use site.
 *
 * For example:
 *
 * ```
 * // Library L1
 * typealias ListAlias<A> = List<A>
 *
 * // Library L2
 * typealias StringAlias = String
 *
 * // Library L3 (depends on L1, L2)
 * val aliasedStringList: ListAlias<StringAlias> = listOf("")
 *
 * // Module M1 (depends ONLY on L1, L3)
 * aliasedStringList
 * ```
 *
 * `StringAlias` is not resolvable in the use-site module M1, so navigation to the type argument of `ListAlias<StringAlias>` should bring us
 * to `String`. But this is only possible if we preserve the type argument's expanded type. If we instead represented `StringAlias` as a
 * [KotlinClassTypeBean] with class ID `StringAlias`, we'd have no chance to resolve the type to anything.
 *
 * Note that in library L3, `ListAlias<StringAlias>` is the abbreviated type of the expanded type `List<String>` which will be written to
 * the library. `List<String>` is decompiled to a [KotlinUserTypeStubImpl] with a [KotlinClassTypeBean] that represents the abbreviated type
 * `ListAlias<StringAlias>`. The internal structure of that [KotlinClassTypeBean] should roughly correspond to the following:
 *
 * ```
 * KotlinClassTypeBean(
 *   classId: "ListAlias",
 *   arguments: listOf(
 *      KotlinClassTypeBean(
 *          classId: "kotlin.String",
 *          abbreviatedType: KotlinClassTypeBean(classId: "StringAlias"),
 *      ),
 *   ),
 * ```
 */
data class KotlinClassTypeBean(
    val classId: ClassId,
    val arguments: List<KotlinTypeArgumentBean>,
    override val nullable: Boolean,
    val abbreviatedType: KotlinClassTypeBean?,
) : KotlinTypeBean, SimpleTypeMarker

data class KotlinTypeArgumentBean(val projectionKind: KtProjectionKind, val type: KotlinTypeBean?) : TypeArgumentMarker

data class KotlinTypeParameterTypeBean(
    val typeParameterName: String,
    override val nullable: Boolean,
    val definitelyNotNull: Boolean
) : KotlinTypeBean, SimpleTypeMarker