/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols.objc

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.OCResolveContext
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMemberSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCPropertySymbol
import com.jetbrains.cidr.lang.symbols.objc.OCPropertySymbol.PropertyAttribute
import com.jetbrains.cidr.lang.symbols.objc.OCPropertySymbol.ValueAttribute
import com.jetbrains.cidr.lang.types.OCType
import com.jetbrains.cidr.lang.types.visitors.OCTypeSubstitution
import org.jetbrains.konan.resolve.translation.toOCType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty

class KtOCPropertySymbol : KtOCMemberSymbol, OCPropertySymbol {
    private lateinit var type: OCType
    private lateinit var attributes: List<String>
    private var externalGetterName: String?
    private var externalSetterName: String?

    constructor(
        stub: ObjCProperty,
        project: Project,
        file: VirtualFile,
        containingClass: OCClassSymbol
    ) : super(stub, stub.name, file, containingClass) {
        this.type = stub.type.toOCType(project, containingClass)
        this.attributes = stub.propertyAttributes
        this.externalGetterName = stub.getterName
        this.externalSetterName = stub.setterName
    }

    constructor() : super() {
        this.externalGetterName = null
        this.externalSetterName = null
    }

    override fun getKind(): OCSymbolKind = OCSymbolKind.PROPERTY

    override fun getSubstitution(): OCTypeSubstitution = OCTypeSubstitution.ID

    override fun isOptional(): Boolean = false

    override fun getAssociatedSymbol(project: Project): OCMemberSymbol? = null //todo ???

    override fun hasAttribute(attribute: PropertyAttribute): Boolean = when (attribute) {
        PropertyAttribute.GETTER -> externalGetterName != null
        PropertyAttribute.SETTER -> externalSetterName != null
        else -> attributes.contains(attribute.tokenName)
    }

    override fun getNameWithParent(context: OCResolveContext): String = "${parent.name}.$name"

    override fun getAttributeValue(attribute: ValueAttribute): String? = when (attribute) {
        ValueAttribute.GETTER -> externalGetterName
        ValueAttribute.SETTER -> externalSetterName
        else -> throw IllegalArgumentException("Unsupported value attribute: $attribute")
    }

    override fun getType(): OCType = type

    override fun getAttributes(): List<String> = super<KtOCMemberSymbol>.getAttributes()
}