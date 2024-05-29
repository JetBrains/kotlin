/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.backend.builders.build
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirDeclarationParent
import org.jetbrains.kotlin.bir.declarations.BirField
import org.jetbrains.kotlin.bir.util.classId
import org.jetbrains.kotlin.bir.util.defaultType
import org.jetbrains.kotlin.bir.util.parentAsClass
import org.jetbrains.kotlin.builtins.CompanionObjectMapping
import org.jetbrains.kotlin.builtins.isMappedIntrinsicCompanionObjectClassId
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name

object JvmCachedDeclarations {
    val FieldForObjectInstance = GlobalBirDynamicProperty<_, BirField>(BirClass)
    val InterfaceCompanionFieldDeclaration = GlobalBirDynamicProperty<_, BirField>(BirClass)
    val FieldForObjectInstanceParent = GlobalBirDynamicProperty<_, BirDeclarationParent>(BirField)

    context(BirBackendContext)
    fun getFieldForObjectInstance(singleton: BirClass): BirField {
        return singleton.getOrPutDynamicProperty(FieldForObjectInstance) {
            val originalVisibility = singleton.visibility
            val isNotMappedCompanion = singleton.isCompanion && !singleton.isMappedIntrinsicCompanionObject()
            val useProperVisibilityForCompanion =
                languageVersionSettings.supportsFeature(LanguageFeature.ProperVisibilityForCompanionObjectInstanceField)
                        && singleton.isCompanion
                        && singleton.parentAsClass.kind != ClassKind.INTERFACE
            BirField.build {
                name = if (isNotMappedCompanion) singleton.name else Name.identifier(JvmAbi.INSTANCE_FIELD)
                type = singleton.defaultType
                origin = IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
                isFinal = true
                isStatic = true
                visibility = when {
                    !useProperVisibilityForCompanion -> DescriptorVisibilities.PUBLIC
                    originalVisibility == DescriptorVisibilities.PROTECTED -> JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY
                    else -> originalVisibility
                }
                this[FieldForObjectInstanceParent] = if (isNotMappedCompanion) singleton.parent as BirDeclarationParent else singleton
            }
        }
    }

    private fun BirClass.isMappedIntrinsicCompanionObject() =
        isCompanion && classId?.let { CompanionObjectMapping.isMappedIntrinsicCompanionObjectClassId(it) } == true

    context(BirBackendContext)
    fun getPrivateFieldForObjectInstance(singleton: BirClass): BirField {
        return if (singleton.isCompanion && singleton.parentAsClass.isJvmInterface)
            singleton.getOrPutDynamicProperty(InterfaceCompanionFieldDeclaration) {
                BirField.build {
                    name = Name.identifier("\$\$INSTANCE")
                    type = singleton.defaultType
                    origin = JvmLoweredDeclarationOrigin.INTERFACE_COMPANION_PRIVATE_INSTANCE
                    isFinal = true
                    isStatic = true
                    visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                    this[FieldForObjectInstanceParent] = singleton
                }
            }
        else {
            getFieldForObjectInstance(singleton)
        }
    }
}