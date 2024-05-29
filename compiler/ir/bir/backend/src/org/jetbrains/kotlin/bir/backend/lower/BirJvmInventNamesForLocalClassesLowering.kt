/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.BirDynamicPropertyKey
import org.jetbrains.kotlin.bir.GlobalBirDynamicProperty
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.backend.jvm.getFileClassInfo
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirFile
import org.jetbrains.kotlin.bir.set
import org.jetbrains.kotlin.bir.util.render
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type

context(JvmBirBackendContext)
class BirJvmInventNamesForLocalClassesLowering() : BirInventNamesForLocalClassesLowering(true, false) {
    override fun computeTopLevelClassName(clazz: BirClass): String {
        val file = clazz.parent as? BirFile
            ?: throw AssertionError("Top-level class expected: ${clazz.render()}")
        val classFqn =
            if (clazz.origin == IrDeclarationOrigin.FILE_CLASS ||
                clazz.origin == IrDeclarationOrigin.SYNTHETIC_FILE_CLASS
            ) {
                file.getFileClassInfo().fileClassFqName
            } else {
                file.packageFqName.child(clazz.name)
            }
        return JvmClassName.byFqNameWithoutInnerClasses(classFqn).internalName
    }

    override fun sanitizeNameIfNeeded(name: String): String {
        return JvmCodegenUtil.sanitizeNameIfNeeded(name, languageVersionSettings)
    }

    override fun putLocalClassName(declaration: BirAttributeContainer, localClassName: String) {
        declaration[LocalClassType] = Type.getObjectType(localClassName)
    }

    companion object {
        val LocalClassType = GlobalBirDynamicProperty<_, Type>(BirAttributeContainer)
    }
}