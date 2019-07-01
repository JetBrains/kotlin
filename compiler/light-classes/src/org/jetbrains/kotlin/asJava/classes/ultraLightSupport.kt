/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver

interface KtUltraLightSupport {
    val moduleName: String
    fun findAnnotation(owner: KtAnnotated, fqName: FqName): Pair<KtAnnotationEntry, AnnotationDescriptor>?
    fun isTooComplexForUltraLightGeneration(element: KtDeclaration): Boolean
    val deprecationResolver: DeprecationResolver
    val typeMapper: KotlinTypeMapper
    val moduleDescriptor: ModuleDescriptor
    val isReleasedCoroutine: Boolean

    companion object {
        // This property may be removed once IntelliJ versions earlier than 2018.3 become unsupported
        // And usages of that property may be replaced with relevant registry key
        @Volatile
        @get:TestOnly
        var forceUsingOldLightClasses = false
    }
}
