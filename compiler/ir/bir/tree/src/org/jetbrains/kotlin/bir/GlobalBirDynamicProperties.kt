/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

object GlobalBirDynamicProperties {
    val Descriptor = GlobalBirDynamicProperty<_, DeclarationDescriptor>(BirSymbolOwner)

    val Metadata = GlobalBirDynamicProperty<_, MetadataSource?>(BirMetadataSourceOwner) // probably rename e.g. to 'source'

    val ContainerSource = GlobalBirDynamicProperty<_, DeserializedContainerSource?>(BirMemberWithContainerSource)

    /*
    If this is a sealed class or interface, this list contains symbols of all its immediate subclasses.
    Otherwise, this is an empty list.

    NOTE: If this was deserialized from a klib, this list will always be empty!
    See [KT-54028](https://youtrack.jetbrains.com/issue/KT-54028).
     */
    val SealedSubclasses = GlobalBirDynamicProperty<_, List<BirClassSymbol>>(BirClass) // Seems only used in JVM

    /*
    Original element before inlining. Useful only with IR inliner.
    `null` if the element wasn't inlined. Unlike [attributeOwnerId], doesn't have the
    idempotence invariant and can contain a chain of declarations.

    null <=> this element wasn't inlined
     */
    val OriginalBeforeInline = GlobalBirDynamicProperty<_, BirAttributeContainer?>(BirAttributeContainer) // Seems only used inside lowering

    val CapturedConstructor = GlobalBirDynamicProperty<_, BirConstructor>(BirConstructor)
}