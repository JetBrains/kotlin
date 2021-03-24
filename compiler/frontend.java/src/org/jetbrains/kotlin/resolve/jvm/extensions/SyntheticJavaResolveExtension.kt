/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.resolve.jvm.CompositeSyntheticJavaPartsProvider
import org.jetbrains.kotlin.resolve.jvm.SyntheticJavaPartsProvider

interface SyntheticJavaResolveExtension {

    companion object : ProjectExtensionDescriptor<SyntheticJavaResolveExtension>(
        "org.jetbrains.kotlin.syntheticJavaResolveExtension", SyntheticJavaResolveExtension::class.java
    ) {
        fun getProvider(project: Project): SyntheticJavaPartsProvider {
            val instances = getInstances(project)
            val providers = instances.map { it.getProvider() }
            return if (providers.isEmpty()) {
                SyntheticJavaPartsProvider.EMPTY
            } else {
                CompositeSyntheticJavaPartsProvider(providers)
            }
        }
    }

    fun getProvider(): SyntheticJavaPartsProvider

}

