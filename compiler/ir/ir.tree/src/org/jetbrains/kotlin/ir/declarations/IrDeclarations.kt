/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation
import org.jetbrains.kotlin.ir.IrAttribute
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.originalBeforeInline
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils.getPackagePartClassNamePrefix
import java.io.File

/**
 * This does two different things:
 *  1. Copies [IrAttribute]s of element [other] into [this]. Attributes already present on [this] are not removed, attributes present
 *  overridden on both [this] and [other] will be overridden in [this]. The semantics is therefore the same as [MutableMap.putAll].
 *  2. Assigns [IrElement.attributeOwnerId] to that of [other].
 *
 *  Now, those two operations are not clearly connected to each other, although they have been historically.
 *  In particular, [IrElement.attributeOwnerId] has lost much of its meaning since, and is _not_ connected to [IrAttribute]s.
 *  But still, _most likely_ you want to do both at the same time, which is what we do here. It should be investigated closer in KT-74295.
 *
 *  @param includeAll if `true`, copy all the attributes present in [other],
 *  if `false`, only those with [IrAttribute.copyByDefault] == `true`.
 */
fun IrElement.copyAttributes(other: IrElement, includeAll: Boolean = false) {
    (this as IrElementBase).copyAttributesFrom(other as IrElementBase, includeAll)
    attributeOwnerId = other.attributeOwnerId
}

val IrClass.isSingleFieldValueClass: Boolean
    get() = valueClassRepresentation is InlineClassRepresentation

val IrClass.isMultiFieldValueClass: Boolean
    get() = valueClassRepresentation is MultiFieldValueClassRepresentation

fun IrClass.addMember(member: IrDeclaration) {
    declarations.add(member)
}

fun IrClass.addAll(members: List<IrDeclaration>) {
    declarations.addAll(members)
}

val IrFile.path: String get() = fileEntry.name
val IrFile.name: String get() = File(path).name
val IrFile.nameWithPackage: String get() = packageFqName.child(Name.identifier(name)).asString()
val IrFile.packagePartClassName: String get() = getPackagePartClassNamePrefix(File(path).nameWithoutExtension) + "Kt"

val IrFunction.isStaticMethodOfClass: Boolean
    get() = this is IrSimpleFunction && parent is IrClass && dispatchReceiverParameter == null

val IrFunction.isPropertyAccessor: Boolean
    get() = this is IrSimpleFunction && correspondingPropertySymbol != null


val IrClass.multiFieldValueClassRepresentation: MultiFieldValueClassRepresentation<IrSimpleType>?
    get() = valueClassRepresentation as? MultiFieldValueClassRepresentation<IrSimpleType>

val IrClass.inlineClassRepresentation: InlineClassRepresentation<IrSimpleType>?
    get() = valueClassRepresentation as? InlineClassRepresentation<IrSimpleType>


@DeprecatedForRemovalCompilerApi(CompilerVersionOfApiDeprecation._2_1_20)
fun <D : IrElement> D.copyAttributes(other: IrElement?): D = apply {
    if (other != null) {
        attributeOwnerId = other.attributeOwnerId
        originalBeforeInline = other.originalBeforeInline
    }
}