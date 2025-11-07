/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.DefaultsDsl
import org.jetbrains.kotlin.test.services.DefaultsProvider
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl.Companion.toArtifactKind
import org.jetbrains.kotlin.util.PrivateForInline

@DefaultsDsl
class DefaultsProviderBuilder {
    lateinit var frontend: FrontendKind<*>
    var targetBackend: TargetBackend? = null
    lateinit var targetPlatform: TargetPlatform
    var backendKind: BackendKind<*>? = null
    var artifactKind: ArtifactKind<*>? = null
    lateinit var dependencyKind: DependencyKind

    @OptIn(PrivateForInline::class)
    fun build(): DefaultsProvider {
        return DefaultsProvider(
            frontend,
            backendKind ?: BackendKinds.fromTargetBackend(targetBackend),
            LanguageVersionSettingsBuilder(),
            targetPlatform,
            artifactKind ?: targetPlatform.toArtifactKind(),
            targetBackend,
            dependencyKind
        )
    }
}
