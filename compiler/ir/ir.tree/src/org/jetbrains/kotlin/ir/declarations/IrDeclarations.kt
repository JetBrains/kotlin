/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.descriptors.BasicValueClassRepresentation
import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassBackendAgnosticApi
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.ir.IrAttribute
import org.jetbrains.kotlin.descriptors.interpretAsInlineClassRepresentationOrNull
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.superClass
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
 * Checks if the class is an inline class or if [treatCompatibleFullValueClassesAsInline] is `true` and
 * the class is a full value class compatible with inline classes.
 *
 * See [ValueClassRepresentation] documentation for more details about value class types and their compatibility.
 *
 * @return `true` if the class is an inline class or if [treatCompatibleFullValueClassesAsInline] is `true` and
 * the class is a full value class compatible with inline classes; otherwise, `false`.
 */
@ValueClassBackendAgnosticApi
fun IrClass.isSingleFieldValueClass(treatCompatibleFullValueClassesAsInline: Boolean): Boolean =
    inlineClassRepresentation(treatCompatibleFullValueClassesAsInline) != null

val IrClass.isFullValueClass: Boolean
    get() = valueClassRepresentation is FullValueClassRepresentation<*>

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

/**
 * Retrieves an [InlineClassRepresentation] of the [IrClass] if the class is an inline class or
 * computes an [InlineClassRepresentation] if [treatCompatibleFullValueClassesAsInline] is `true` and
 * the class is a compatible full value class.
 *
 * See [ValueClassRepresentation] documentation for more details about value class types and their compatibility.
 *
 * @return An [InlineClassRepresentation] or `null`.
 */
@ValueClassBackendAgnosticApi
fun IrClass.inlineClassRepresentation(treatCompatibleFullValueClassesAsInline: Boolean): InlineClassRepresentation<IrSimpleType>? =
    valueClassRepresentation?.interpretAsInlineClassRepresentationOrNull(
        treatCompatibleFullValueClassesAsInline = treatCompatibleFullValueClassesAsInline,
        hasSuperClass = { superClass != null }
    )


@DeprecatedForRemovalCompilerApi(CompilerVersionOfApiDeprecation._2_1_20)
fun <D : IrElement> D.copyAttributes(other: IrElement?): D = apply {
    if (other != null) {
        copyAttributes(other, includeAll = false)
    }
}
