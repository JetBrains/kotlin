/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

import com.intellij.psi.stubs.StubRegistry
import com.intellij.psi.stubs.StubRegistryExtension
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.factories.*

/**
 * Associates Kotlin stub serializers and factories with their element types, decoupling stub support from the
 * element types themselves.
 */
internal class KotlinStubRegistryExtension : StubRegistryExtension {
    @OptIn(KtImplementationDetail::class)
    override fun register(registry: StubRegistry) {
        registry.registerStubSerializer(KtFileElementType, KtFileStubSerializer)

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.SECONDARY_CONSTRUCTOR,
            factory = KtSecondaryConstructorStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.PRIMARY_CONSTRUCTOR,
            factory = KtPrimaryConstructorStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.CLASS,
            factory = KtClassStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.FUNCTION,
            factory = KtFunctionStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.PROPERTY,
            factory = KtPropertyStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.PROPERTY_ACCESSOR,
            factory = KtPropertyAccessorStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.BACKING_FIELD,
            factory = KtBackingFieldStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.DESTRUCTURING_DECLARATION,
            factory = KtDestructuringDeclarationStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.TYPEALIAS,
            factory = KtTypeAliasStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.ENUM_ENTRY,
            factory = KtEnumEntryStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.OBJECT_DECLARATION,
            factory = KtObjectStubSerializingElementFactory,
        )
    }
}
