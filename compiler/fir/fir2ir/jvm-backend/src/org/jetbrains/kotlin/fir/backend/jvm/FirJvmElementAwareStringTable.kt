/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.ClassId

class FirJvmElementAwareStringTable(
    nameResolver: JvmNameResolver? = null
) : JvmStringTable(nameResolver), FirElementAwareStringTable {
    override fun getLocalClassIdReplacement(classLikeDeclaration: FirClassLikeDeclaration<*>): ClassId {
        return when (classLikeDeclaration.symbol.classId.outerClassId) {
            // TODO: how to determine parent declaration for FIR local class properly?
            //is ClassifierDescriptorWithTypeParameters -> getLocalClassIdReplacement(container).createNestedClassId(descriptor.name)
//            null -> {
//                throw IllegalStateException(
//                    "getLocalClassIdReplacement should only be called for local classes: ${classLikeDeclaration.render()}"
//                )
//            }
            else -> {
                classLikeDeclaration.symbol.classId
                // TODO: typeMapper.mapClass
                //val fqName = FqName(typeMapper.mapClass(descriptor).internalName.replace('/', '.'))
                //ClassId(fqName.parent(), FqName.topLevel(fqName.shortName()), true)
            }
        }
    }
}