/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.compilation.KaCompiledClassHandler
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.DelegatingClassBuilderFactory
import org.jetbrains.kotlin.ir.PsiSourceManager
import org.jetbrains.kotlin.ir.declarations.IrClass

@KaImplementationDetail
class KaClassBuilderFactory private constructor(
    private val delegateFactory: ClassBuilderFactory,
    private val compiledClassHandler: KaCompiledClassHandler
) : DelegatingClassBuilderFactory(delegateFactory) {
    @KaImplementationDetail
    companion object {
        fun create(delegateFactory: ClassBuilderFactory, compiledClassHandler: KaCompiledClassHandler?): ClassBuilderFactory {
            return if (compiledClassHandler != null) {
                KaClassBuilderFactory(delegateFactory, compiledClassHandler)
            } else {
                delegateFactory
            }
        }
    }

    override fun newClassBuilder(origin: IrClass?): DelegatingClassBuilder {
        val delegateClassBuilder = delegateFactory.newClassBuilder(origin)

        return object : DelegatingClassBuilder() {
            override fun getDelegate(): ClassBuilder = delegateClassBuilder

            override fun defineClass(
                version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String?>,
            ) {
                val element = origin?.let(PsiSourceManager::findPsiElement)
                compiledClassHandler.handleClassDefinition(element?.containingFile, name)
                super.defineClass(version, access, name, signature, superName, interfaces)
            }
        }
    }
}
