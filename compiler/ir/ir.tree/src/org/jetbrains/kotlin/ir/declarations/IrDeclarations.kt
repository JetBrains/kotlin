/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.descriptors.BasicValueClassRepresentation
import org.jetbrains.kotlin.descriptors.ExtendedValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.JvmInlineMultiFieldValueClassRepresentation
import org.jetbrains.kotlin.ir.IrAttribute
import org.jetbrains.kotlin.descriptors.toInlineRepresentation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
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

/**
 * Determines whether the current `IrClass` is compatible with being a single-field value class.
 *
 * The compatibility is assessed based on the type of value class representation associated with the `IrClass`.
 *
 * @param distinguishBasicAndExtended A boolean indicating whether to differentiate between basic and extended value class representations.
 *                                    If `true`, `ExtendedValueClassRepresentation` will not be considered as single-field compatible,
 *                                    regardless of the number of properties in the representation. If `false`, the compatibility
 *                                    for extended value classes depends on whether they have exactly one underlying property.
 *                                    `true` must be used for JVM, `false` for other backends.
 * @return `true` if the `IrClass` is compatible with being a single-field value class; `false` otherwise.
 */
fun IrClass.isSingleFieldValueClass(distinguishBasicAndExtended: Boolean): Boolean =
    valueClassRepresentation?.toInlineRepresentation(distinguishBasicAndExtended) != null

val IrClass.isJvmInlineMultiFieldValueClass: Boolean
    get() = valueClassRepresentation is JvmInlineMultiFieldValueClassRepresentation

val IrClass.isExtendedValueClass: Boolean
    get() = valueClassRepresentation is ExtendedValueClassRepresentation<*>

val IrClass.isBasicValueClass: Boolean
    get() = valueClassRepresentation is BasicValueClassRepresentation<*>

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


val IrClass.jvmInlineMultiFieldValueClassRepresentation: JvmInlineMultiFieldValueClassRepresentation<IrSimpleType>?
    get() = valueClassRepresentation as? JvmInlineMultiFieldValueClassRepresentation<IrSimpleType>

/**
 * Retrieves the inline class representation of this class if available.
 *
 * This method evaluates the type of the class's value class representation and
 * determines whether to return its equivalent inline class representation.
 *
 * @param distinguishBasicAndExtended A boolean indicating whether to differentiate between basic and extended value class representations.
 *                                    If `true`, `ExtendedValueClassRepresentation` will not be considered as single-field compatible,
 *                                    regardless of the number of properties in the representation. If `false`, the compatibility
 *                                    for extended value classes depends on whether they have exactly one underlying property.
 *                                    `true` must be used for JVM, `false` for other backends.
 * @return An [InlineClassRepresentation] if the class has a compatible value class
 *         representation and meets the conditions specified by the `distinguishBasicAndExtended`
 *         parameter; otherwise, `null`.
 */
fun IrClass.inlineClassRepresentation(distinguishBasicAndExtended: Boolean): InlineClassRepresentation<IrSimpleType>? =
    valueClassRepresentation?.toInlineRepresentation(distinguishBasicAndExtended)


@DeprecatedForRemovalCompilerApi(CompilerVersionOfApiDeprecation._2_1_20)
fun <D : IrElement> D.copyAttributes(other: IrElement?): D = apply {
    if (other != null) {
        copyAttributes(other, includeAll = false)
    }
}
