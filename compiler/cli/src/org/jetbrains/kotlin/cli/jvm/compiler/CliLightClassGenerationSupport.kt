/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.asJava.LightClassBuilder
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.InvalidLightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolderImpl
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * This class solves the problem of interdependency between analyzing Kotlin code and generating JetLightClasses

 * Consider the following example:

 * KClass.kt refers to JClass.java and vice versa

 * To analyze KClass.kt we need to load descriptors from JClass.java, and to do that we need a JetLightClass instance for KClass,
 * which can only be constructed when the structure of KClass is known.

 * To mitigate this, CliLightClassGenerationSupport hold a trace that is shared between the analyzer and JetLightClasses
 */
class CliLightClassGenerationSupport(private val traceHolder: CliTraceHolder) : LightClassGenerationSupport() {
    override fun createUltraLightClass(element: KtClassOrObject) = null

    override fun createDataHolderForClass(classOrObject: KtClassOrObject, builder: LightClassBuilder): LightClassDataHolder.ForClass {
        //force resolve companion for light class generation
        traceHolder.bindingContext.get(BindingContext.CLASS, classOrObject)?.companionObjectDescriptor

        val (stub, bindingContext, diagnostics) = builder(getContext())

        bindingContext.get(BindingContext.CLASS, classOrObject) ?: return InvalidLightClassDataHolder

        return LightClassDataHolderImpl(stub, diagnostics)
    }

    override fun createDataHolderForFacade(files: Collection<KtFile>, builder: LightClassBuilder): LightClassDataHolder.ForFacade {
        val (stub, _, diagnostics) = builder(getContext())
        return LightClassDataHolderImpl(stub, diagnostics)
    }

    override fun createDataHolderForScript(script: KtScript, builder: LightClassBuilder): LightClassDataHolder.ForScript {
        val (stub, _, diagnostics) = builder(getContext())
        return LightClassDataHolderImpl(stub, diagnostics)
    }

    private fun getContext(): LightClassConstructionContext = LightClassConstructionContext(traceHolder.bindingContext, traceHolder.module)

    override fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor? {
        return traceHolder.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
    }

    override fun analyze(element: KtElement) = traceHolder.bindingContext

    override fun analyzeWithContent(element: KtClassOrObject) = traceHolder.bindingContext
}
