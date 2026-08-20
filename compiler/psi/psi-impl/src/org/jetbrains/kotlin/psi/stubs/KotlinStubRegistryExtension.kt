/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubRegistry
import com.intellij.psi.stubs.StubRegistryExtension
import com.intellij.psi.tree.IElementType
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

        registry.registerPlaceHolderFactory(
            type = KtStubElementTypes.CLASS_INITIALIZER,
            psiFactory = ::KtClassInitializer,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.SCRIPT_INITIALIZER,
            factory = KtScriptInitializerStubSerializingElementFactory,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.VALUE_PARAMETER,
            factory = KtParameterStubSerializingElementFactory,
        )

        registry.registerPlaceHolderFactory(
            type = KtStubElementTypes.VALUE_PARAMETER_LIST,
            psiFactory = ::KtParameterList,
        )

        registry.registerStubSerializingFactory(
            type = KtStubElementTypes.TYPE_PARAMETER,
            factory = KtTypeParameterStubSerializingElementFactory,
        )

        registry.registerPlaceHolderFactory(
            type = KtStubElementTypes.TYPE_PARAMETER_LIST,
            psiFactory = ::KtTypeParameterList,
        )
    }
}

/**
 * Registers a factory for an element whose stub carries no data beyond its own presence.
 */
private fun <Psi : KtElementImplStub<out StubElement<*>>> StubRegistry.registerPlaceHolderFactory(
    type: IElementType,
    psiFactory: (KotlinPlaceHolderStub<Psi>) -> Psi,
) {
    registerStubSerializingFactory(type, KtPlaceHolderStubSerializingElementFactory(type, psiFactory))
}
