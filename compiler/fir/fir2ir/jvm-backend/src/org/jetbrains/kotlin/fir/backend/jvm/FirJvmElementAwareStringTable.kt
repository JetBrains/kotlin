/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirJvmElementAwareStringTable(
    private val typeMapper: IrTypeMapper,
    private val components: Fir2IrComponents,
    private val localPoppedUpClasses: List<IrAttributeContainer>,
    nameResolver: JvmNameResolver? = null
) : JvmStringTable(nameResolver), FirElementAwareStringTable {
    override fun getLocalClassIdReplacement(firClass: FirClass): ClassId =
        components.classifierStorage.getCachedIrClass(firClass)?.getLocalClassIdReplacement()
            ?: throw AssertionError("not a local class: ${firClass.symbol.classId}")

    private fun IrClass.getLocalClassIdReplacement(): ClassId {
        // This convoluted implementation aims to reproduce K1 behaviour (see JvmCodegenStringTable::getLocalClassIdReplacement).
        // K1 uses both '.' and '$' to separate nested class names when serializing metadata.
        // That does not make much sense and looks like an artifact of implementation arising from K1 descriptors structure.
        // But we still need to preserve it to establish compatibility with K2-produced metadata.

        val thisClassName = typeMapper.classInternalName(this).replace('/', '.')
        if (attributeOwnerId in localPoppedUpClasses) {
            // For those classes, whose original parent has been changed on the lowering stage.
            // In K1, the `containingDeclaration` of such class descriptor would be the original declaration: constructor or property.
            // Thus, this case corresponds to the `else` branch of JvmCodegenStringTable::getLocalClassIdReplacement.
            val thisClassFqName = FqName(thisClassName)
            return ClassId(thisClassFqName.parent(), FqName.topLevel(thisClassFqName.shortName()), isLocal = true)
        } else {
            // Otherwise, find topmost class parent. Its name will have '$' as delimiter
            val topmostClassParent = generateSequence(this) { it.parent as? IrClass }.last()
            val topmostClassParentName = typeMapper.classInternalName(topmostClassParent).replace('/', '.')
            val prefixFqName = FqName(topmostClassParentName)
            var classId = ClassId(prefixFqName.parent(), FqName.topLevel(prefixFqName.shortName()), isLocal = true)
            if (thisClassName.length == topmostClassParentName.length) return classId
            // The remaining part uses '.'
            thisClassName.substring(topmostClassParentName.length + 1).split('$').forEach {
                classId = classId.createNestedClassId(Name.identifier(it))
            }
            return classId
        }
    }
}
