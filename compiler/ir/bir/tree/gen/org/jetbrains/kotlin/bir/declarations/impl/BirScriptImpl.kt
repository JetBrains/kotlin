/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.BirStatement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirScriptSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirScriptImpl @ObsoleteDescriptorBasedAPI constructor(
    override var sourceSpan: SourceSpan,
    @property:ObsoleteDescriptorBasedAPI
    override val descriptor: DeclarationDescriptor,
    override var signature: IdSignature,
    override var annotations: List<BirConstructorCall>,
    override var origin: IrDeclarationOrigin,
    override var parent: BirDeclarationParent,
    override var name: Name,
    override val statements: MutableList<BirStatement>,
    override var metadata: MetadataSource?,
    override var thisReceiver: BirValueParameter?,
    override var baseClass: BirType?,
    override var explicitCallParameters: List<BirVariable>,
    override var implicitReceiversParameters: List<BirValueParameter>,
    override var providedProperties: List<BirPropertySymbol>,
    override var providedPropertiesParameters: List<BirValueParameter>,
    override var resultProperty: BirPropertySymbol?,
    override var earlierScriptsParameter: BirValueParameter?,
    override var importedScripts: List<BirScriptSymbol>?,
    override var earlierScripts: List<BirScriptSymbol>?,
    override var targetClass: BirClassSymbol?,
    override var constructor: BirConstructor?,
) : BirScript()
