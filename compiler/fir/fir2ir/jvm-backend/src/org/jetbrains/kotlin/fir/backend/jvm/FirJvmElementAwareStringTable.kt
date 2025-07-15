/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.isEnclosedInConstructor
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

class FirJvmElementAwareStringTable(
    private val typeMapper: IrTypeMapper,
    private val components: Fir2IrComponents,
    nameResolver: JvmNameResolver? = null
) : JvmStringTable(nameResolver), FirElementAwareStringTable {
    override fun getLocalClassLikeDeclarationIdReplacement(declaration: FirClassLikeDeclaration): ClassId {
        if (components.configuration.skipBodies) {
            return when (declaration) {
                is FirClass -> declaration.superConeTypes.firstOrNull()?.lookupTag?.classId ?: StandardClassIds.Any
                is FirTypeAlias -> StandardClassIds.Any
            }
        }
        // TODO: should call getCachedIrLocalClass, see KT-66018
        val irClassLikeDeclaration = when (declaration) {
            is FirClass -> components.classifierStorage.getIrClass(declaration)
            is FirTypeAlias -> components.classifierStorage.getIrTypeAliasSymbol(declaration.symbol).owner
        }
        return irClassLikeDeclaration.getLocalClassLikeDeclarationIdReplacement()
    }

    /**
     * The method aims to reproduce convoluted K1 behavior
     * (@see [org.jetbrains.kotlin.codegen.serialization.JvmCodegenStringTable.getLocalClassIdReplacement]).
     * K1 uses both `.` and `$` to separate nested class names when serializing metadata.
     * That makes little sense and looks like an artifact of implementation arising from the K1 descriptors structure.
     * But we still need to preserve it to establish compatibility with K2-produced metadata.
     */
    private fun IrDeclarationBase.getLocalClassLikeDeclarationIdReplacement(): ClassId {
        val thisClassLikeDeclarationName = typeMapper.classLikeDeclarationInternalName(this).replace('/', '.')
        if (isEnclosedInConstructor) {
            // For those classes, whose original parent has been changed on the lowering stage.
            // In K1, the `containingDeclaration` of such class descriptor would be the original declaration: constructor or property.
            // Thus, this case corresponds to the `else` branch of JvmCodegenStringTable::getLocalClassIdReplacement.
            val thisClassLikeDeclarationFqName = FqName(thisClassLikeDeclarationName)
            return ClassId(
                thisClassLikeDeclarationFqName.parent(),
                FqName.topLevel(thisClassLikeDeclarationFqName.shortName()),
                isLocal = true
            )
        } else {
            // Otherwise, find the topmost class parent. Its name will have '$' as a delimiter
            val topmostClassParent = generateSequence(this) { it.parent as? IrClass }.last()
            val topmostClassParentName = typeMapper.classLikeDeclarationInternalName(topmostClassParent).replace('/', '.')
            val prefixFqName = FqName(topmostClassParentName)
            var classId = ClassId(prefixFqName.parent(), FqName.topLevel(prefixFqName.shortName()), isLocal = true)
            if (thisClassLikeDeclarationName.length == topmostClassParentName.length) return classId
            // The remaining part uses '.'
            thisClassLikeDeclarationName.substring(topmostClassParentName.length + 1).split('$').forEach {
                classId = classId.createNestedClassId(Name.identifier(it))
            }
            return classId
        }
    }
}
