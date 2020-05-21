/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import java.util.concurrent.ConcurrentMap

interface KtUltraLightSupport {
    val moduleName: String
    val deprecationResolver: DeprecationResolver
    val typeMapper: KotlinTypeMapper
    val moduleDescriptor: ModuleDescriptor
    val isReleasedCoroutine: Boolean
    fun hasAlias(file: KtFile, shortName: Name?): Boolean = false

    companion object {
        // This property may be removed once IntelliJ versions earlier than 2018.3 become unsupported
        // And usages of that property may be replaced with relevant registry key
        @Volatile
        @get:TestOnly
        var forceUsingOldLightClasses = false
    }
}

class UltraLightSupportViaService(private val ktElement: KtElement) : KtUltraLightSupport {

    private val serviceProvidedSupport: KtUltraLightSupport
        get() = LightClassGenerationSupport.getInstance(ktElement.project).getUltraLightClassSupport(ktElement)

    override val moduleName: String
        get() = serviceProvidedSupport.moduleName

    override val deprecationResolver: DeprecationResolver
        get() = serviceProvidedSupport.deprecationResolver

    override val typeMapper: KotlinTypeMapper
        get() = serviceProvidedSupport.typeMapper

    override val moduleDescriptor: ModuleDescriptor
        get() = serviceProvidedSupport.moduleDescriptor

    override val isReleasedCoroutine: Boolean
        get() = serviceProvidedSupport.isReleasedCoroutine
}
