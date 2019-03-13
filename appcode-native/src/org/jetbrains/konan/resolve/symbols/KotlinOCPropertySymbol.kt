/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.symbols.OCResolveContext
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMemberSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCPropertySymbol
import com.jetbrains.cidr.lang.types.OCType
import com.jetbrains.cidr.lang.types.visitors.OCTypeSubstitution
import org.jetbrains.konan.resolve.toOCType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty

class KotlinOCPropertySymbol(
    stub: ObjCProperty,
    project: Project,
    containingClass: OCClassSymbol
) : KotlinOCMemberSymbol<ObjCProperty>(stub, project, containingClass), OCPropertySymbol {

    private val myType: OCType = stub.type.toOCType(project, containingClass)

    override fun getKind(): OCSymbolKind = OCSymbolKind.PROPERTY

    override fun getSubstitution(): OCTypeSubstitution = OCTypeSubstitution.ID

    override fun isOptional(): Boolean = false

    override fun getAssociatedSymbol(project: Project): OCMemberSymbol? = null //todo ???

    override fun hasAttribute(attribute: OCPropertySymbol.PropertyAttribute): Boolean {
        return stub.attributes.contains(attribute.tokenName) ||
                attribute == OCPropertySymbol.PropertyAttribute.GETTER && stub.getterName != null ||
                attribute == OCPropertySymbol.PropertyAttribute.SETTER && stub.setterName != null
    }

    override fun getNameWithParent(context: OCResolveContext): String {
        return "${parent.name}.$name"
    }

    override fun getAttributeValue(attribute: OCPropertySymbol.ValueAttribute): String? {
        return when (attribute) {
            OCPropertySymbol.ValueAttribute.GETTER -> stub.getterName
            OCPropertySymbol.ValueAttribute.SETTER -> stub.setterName
            else -> throw IllegalArgumentException("Unsupported value attribute: $attribute")
        }
    }

    override fun getType(): OCType = myType
}