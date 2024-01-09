/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.builders.build
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.declarations.BirTypeAlias
import org.jetbrains.kotlin.bir.expressions.impl.BirBlockBodyImpl
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name

context(JvmBirBackendContext)
class BirTypeAliasAnnotationMethodsLowering : BirLoweringPhase() {
    private val annotatedTypeAliases = registerIndexKey<BirTypeAlias>(BirTypeAlias, false) { it.annotations.isNotEmpty() }

    override fun lower(module: BirModuleFragment) {
        getAllElementsWithIndex(annotatedTypeAliases).forEach { alias ->
            val parentClass = alias.parent as? BirClass ?: return@forEach

            val function = BirSimpleFunction.build {
                name = Name.identifier(JvmAbi.getSyntheticMethodNameForAnnotatedTypeAlias(alias.name))
                visibility = alias.visibility
                returnType = birBuiltIns.unitType
                modality = Modality.OPEN
                origin = JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS
                body = BirBlockBodyImpl(SourceSpan.UNDEFINED)
                annotations += alias.annotations
            }
            parentClass.declarations += function
        }
    }
}