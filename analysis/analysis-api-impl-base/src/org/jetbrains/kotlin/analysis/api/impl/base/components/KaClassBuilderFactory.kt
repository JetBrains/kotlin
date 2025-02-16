/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaCompiledClassHandler
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin

@KaImplementationDetail
class KaClassBuilderFactory private constructor(
    private val delegateFactory: ClassBuilderFactory,
    private val compiledClassHandler: KaCompiledClassHandler
) : DelegatingClassBuilderFactory(delegateFactory) {
    companion object {
        fun create(delegateFactory: ClassBuilderFactory, compiledClassHandler: KaCompiledClassHandler?): ClassBuilderFactory {
            return if (compiledClassHandler != null) {
                KaClassBuilderFactory(delegateFactory, compiledClassHandler)
            } else {
                delegateFactory
            }
        }
    }

    override fun newClassBuilder(origin: JvmDeclarationOrigin): DelegatingClassBuilder {
        val delegateClassBuilder = delegateFactory.newClassBuilder(origin)

        return object : DelegatingClassBuilder() {
            override fun getDelegate(): ClassBuilder = delegateClassBuilder

            override fun defineClass(
                psi: PsiElement?, version: Int, access: Int, name: String, signature: String?, superName: String,
                interfaces: Array<out String?>,
            ) {
                compiledClassHandler.handleClassDefinition(origin.element?.containingFile, name)
                super.defineClass(psi, version, access, name, signature, superName, interfaces)
            }
        }
    }
}