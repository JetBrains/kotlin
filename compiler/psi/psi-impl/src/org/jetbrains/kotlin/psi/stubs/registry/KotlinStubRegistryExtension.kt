/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.registry

import com.intellij.psi.stubs.StubRegistry
import com.intellij.psi.stubs.StubRegistryExtension
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

/**
 * Associates Kotlin stub serializers and factories with their element types, decoupling stub support from the
 * element types themselves (KT-78356).
 *
 * Every [KtStubElementType] declared in [KtStubElementTypes] is registered with its [KtStubElementType.getStubFactory]
 * and [KtStubElementType.getStubSerializer]; non-stub element types such as code fragments are skipped automatically.
 */
@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal class KotlinStubRegistryExtension : StubRegistryExtension {
    override fun register(registry: StubRegistry) {
        registry.registerStubSerializer(KtFileElementType, KotlinFileStubSerializer())

        for (field in KtStubElementTypes::class.java.fields) {
            val elementType = field.get(null) as? KtStubElementType<*, *> ?: continue
            registry.registerStubFactory(elementType, elementType.stubFactory)
            registry.registerStubSerializer(elementType, elementType.stubSerializer)
        }
    }
}
