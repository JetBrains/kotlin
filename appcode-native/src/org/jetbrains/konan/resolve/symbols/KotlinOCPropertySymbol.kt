/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
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
    file: VirtualFile,
    containingClass: OCClassSymbol
) : KotlinOCMemberSymbol(stub, file, containingClass), OCPropertySymbol {

    private val type: OCType = stub.type.toOCType(project, containingClass)
    private val attributes: List<String> = stub.propertyAttributes
    private val getterName: String? = stub.getterName
    private val setterName: String? = stub.setterName

    override fun getKind(): OCSymbolKind = OCSymbolKind.PROPERTY

    override fun getSubstitution(): OCTypeSubstitution = OCTypeSubstitution.ID

    override fun isOptional(): Boolean = false

    override fun getAssociatedSymbol(project: Project): OCMemberSymbol? = null //todo ???

    override fun hasAttribute(attribute: OCPropertySymbol.PropertyAttribute): Boolean {
        return attributes.contains(attribute.tokenName) ||
               attribute == OCPropertySymbol.PropertyAttribute.GETTER && getterName != null ||
               attribute == OCPropertySymbol.PropertyAttribute.SETTER && setterName != null
    }

    override fun getNameWithParent(context: OCResolveContext): String = "${parent.name}.$name"

    override fun getAttributeValue(attribute: OCPropertySymbol.ValueAttribute): String? {
        return when (attribute) {
            OCPropertySymbol.ValueAttribute.GETTER -> getterName
            OCPropertySymbol.ValueAttribute.SETTER -> setterName
            else -> throw IllegalArgumentException("Unsupported value attribute: $attribute")
        }
    }

    override fun getType(): OCType = type

    override fun getAttributes(): List<String> = super<KotlinOCMemberSymbol>.getAttributes()
    override fun getGetterName(): String = super.getGetterName()
    override fun getSetterName(): String = super.getSetterName()
}