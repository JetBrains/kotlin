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

fun IrElement.copyAttributes(other: IrElement, includeAll: Boolean = false) {
    (this as IrElementBase).copyAttributesFrom(other as IrElementBase, includeAll)
    attributeOwnerId = other.attributeOwnerId
    originalBeforeInline = other.originalBeforeInline
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