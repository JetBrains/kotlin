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

import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import org.jetbrains.kotlin.asJava.LightClassBuilder
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.InvalidLightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolderImpl
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClassForScript
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.components.JavaDeprecationSettings
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.deprecation.CoroutineCompatibilitySupport
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType

/**
 * This class solves the problem of interdependency between analyzing Kotlin code and generating JetLightClasses

 * Consider the following example:

 * KClass.kt refers to JClass.java and vice versa

 * To analyze KClass.kt we need to load descriptors from JClass.java, and to do that we need a JetLightClass instance for KClass,
 * which can only be constructed when the structure of KClass is known.

 * To mitigate this, CliLightClassGenerationSupport hold a trace that is shared between the analyzer and JetLightClasses
 */
class CliLightClassGenerationSupport(private val traceHolder: CliTraceHolder) : LightClassGenerationSupport() {

    private val ultraLightSupport = object : KtUltraLightSupport {

        private val languageVersionSettings: LanguageVersionSettings
            get() = getContext().languageVersionSettings ?: LanguageVersionSettingsImpl.DEFAULT

        override val isReleasedCoroutine
            get() = languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)

        override val moduleDescriptor get() = traceHolder.module

        override val moduleName: String get() = JvmCodegenUtil.getModuleName(moduleDescriptor)

        override val deprecationResolver: DeprecationResolver
            get() = DeprecationResolver(
                LockBasedStorageManager.NO_LOCKS,
                languageVersionSettings,
                CoroutineCompatibilitySupport.ENABLED,
                JavaDeprecationSettings
            )

        override val typeMapper: KotlinTypeMapper by lazyPub {
            KotlinTypeMapper(
                BindingContext.EMPTY,
                ClassBuilderMode.LIGHT_CLASSES,
                moduleName,
                languageVersionSettings,
                jvmTarget = JvmTarget.JVM_1_8,
                typePreprocessor = KotlinType::cleanFromAnonymousTypes,
                namePreprocessor = ::tryGetPredefinedName
            )
        }
    }

    override fun createUltraLightClassForFacade(
        manager: PsiManager,
        facadeClassFqName: FqName,
        lightClassDataCache: CachedValue<LightClassDataHolder.ForFacade>,
        files: Collection<KtFile>
    ): KtUltraLightClassForFacade? {

        if (files.any { it.isScript() }) return null

        val filesToSupports: List<Pair<KtFile, KtUltraLightSupport>> = files.map {
            it to UltraLightSupportViaService(it)
        }

        return KtUltraLightClassForFacade(
            manager,
            facadeClassFqName,
            lightClassDataCache,
            files,
            filesToSupports
        )
    }

    override fun createUltraLightClass(element: KtClassOrObject): KtUltraLightClass? {
        if (element.shouldNotBeVisibleAsLightClass()) {
            return null
        }

        return UltraLightSupportViaService(element).let { support ->
            when {
                element is KtObjectDeclaration && element.isObjectLiteral() ->
                    KtUltraLightClassForAnonymousDeclaration(element, support)
                element.safeIsLocal() ->
                    KtUltraLightClassForLocalDeclaration(element, support)

                (element.hasModifier(KtTokens.INLINE_KEYWORD)) ->
                    KtUltraLightInlineClass(element, support)

                else -> KtUltraLightClass(element, support)
            }
        }
    }

    override fun createUltraLightClassForScript(script: KtScript): KtUltraLightClassForScript? =
        KtUltraLightClassForScript(script, support = UltraLightSupportViaService(script))

    override fun getUltraLightClassSupport(element: KtElement): KtUltraLightSupport = ultraLightSupport

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

    override fun analyzeAnnotation(element: KtAnnotationEntry) = traceHolder.bindingContext.get(BindingContext.ANNOTATION, element)

    override fun analyzeWithContent(element: KtClassOrObject) = traceHolder.bindingContext
}