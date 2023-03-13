/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.KtUltraLightSupport
import org.jetbrains.kotlin.asJava.classes.cleanFromAnonymousTypes
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.classes.tryGetPredefinedName
import org.jetbrains.kotlin.cli.jvm.compiler.builder.LightClassConstructionContext
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.java.components.JavaDeprecationSettings
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType

/**
 * This class solves the problem of interdependency between analyzing Kotlin code and generating KotlinLightClasses

 * Consider the following example:

 * KClass.kt refers to JClass.java and vice versa

 * To analyze KClass.kt we need to load descriptors from JClass.java, and to do that we need a JetLightClass instance for KClass,
 * which can only be constructed when the structure of KClass is known.

 * To mitigate this, CliLightClassGenerationSupport hold a trace that is shared between the analyzer and JetLightClasses
 */
class CliLightClassGenerationSupport(
    val traceHolder: CliTraceHolder,
    private val project: Project
) : LightClassGenerationSupport() {

    private class CliLightClassSupport(
        private val project: Project,
        override val languageVersionSettings: LanguageVersionSettings,
        override val jvmTarget: JvmTarget
    ) : KtUltraLightSupport {

        // This is the way to untie CliLightClassSupport and CliLightClassGenerationSupport to prevent descriptors leak
        private val traceHolder: CliTraceHolder
            get() = (getInstance(project) as CliLightClassGenerationSupport).traceHolder

        override fun possiblyHasAlias(file: KtFile, shortName: Name): Boolean = true

        override val moduleDescriptor get() = traceHolder.module

        override val moduleName: String get() = JvmCodegenUtil.getModuleName(moduleDescriptor)

        override val deprecationResolver: DeprecationResolver
            get() = DeprecationResolver(
                LockBasedStorageManager.NO_LOCKS,
                languageVersionSettings,
                JavaDeprecationSettings
            )

        override val typeMapper: KotlinTypeMapper by lazyPub {
            KotlinTypeMapper(
                BindingContext.EMPTY,
                ClassBuilderMode.LIGHT_CLASSES,
                moduleName,
                languageVersionSettings,
                useOldInlineClassesManglingScheme = false,
                jvmTarget = jvmTarget,
                typePreprocessor = KotlinType::cleanFromAnonymousTypes,
                namePreprocessor = ::tryGetPredefinedName
            )
        }
    }

    private val ultraLightSupport: KtUltraLightSupport by lazyPub {
        CliLightClassSupport(project, traceHolder.languageVersionSettings, traceHolder.jvmTarget)
    }

    override fun getUltraLightClassSupport(element: KtElement): KtUltraLightSupport {
        require(element.project == project) { "ULC support created from another project from requested" }
        return ultraLightSupport
    }

    val context: LightClassConstructionContext
        get() = LightClassConstructionContext(
            traceHolder.bindingContext,
            traceHolder.module,
            traceHolder.languageVersionSettings,
            traceHolder.jvmTarget,
        )

    override fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor? {
        return traceHolder.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
    }

    override fun analyze(element: KtElement) = traceHolder.bindingContext

    override fun analyzeAnnotation(element: KtAnnotationEntry) = traceHolder.bindingContext.get(BindingContext.ANNOTATION, element)

    override fun analyzeWithContent(element: KtClassOrObject) = traceHolder.bindingContext
}
