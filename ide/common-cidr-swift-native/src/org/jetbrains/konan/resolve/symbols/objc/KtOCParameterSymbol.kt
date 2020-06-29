/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols.objc

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.*
import com.jetbrains.cidr.lang.symbols.cpp.OCDeclaratorSymbol
import com.jetbrains.cidr.lang.symbols.cpp.OCSymbolWithQualifiedName
import com.jetbrains.cidr.lang.symbols.expression.OCExpressionSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.types.OCType
import com.jetbrains.cidr.lang.types.OCTypeArgument
import com.jetbrains.cidr.lang.types.visitors.OCTypeSubstitution
import org.jetbrains.konan.resolve.translation.toOCType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty

class KtOCParameterSymbol : KtOCImmediateSymbol, OCDeclaratorSymbol {
    private lateinit var containingClass: OCClassSymbol
    private lateinit var qualifiedName: OCQualifiedName
    private lateinit var type: OCType

    constructor(
        stub: ObjCParameter,
        project: Project,
        file: VirtualFile,
        containingClass: OCClassSymbol
    ) : super(stub, stub.name, file) {
        this.containingClass = containingClass
        this.qualifiedName = OCQualifiedName.interned(name)
        this.type = stub.type.toOCType(project, containingClass)
    }

    constructor(
        property: KtOCPropertySymbol,
        stub: ObjCProperty,
        file: VirtualFile,
        containingClass: OCClassSymbol
    ) : super(stub, stub.name, file) {
        this.containingClass = containingClass
        this.qualifiedName = OCQualifiedName.interned(name)
        this.type = property.type
    }

    constructor() : super()

    override fun getKind(): OCSymbolKind = OCSymbolKind.PARAMETER

    override fun isGlobal(): Boolean = false

    override fun hasAttribute(attribute: OCSymbolAttribute): Boolean = false

    override fun getSubstitution(): OCTypeSubstitution = OCTypeSubstitution.ID

    override fun getVisibility(): OCVisibility? = null

    override fun isTemplateSymbol(): Boolean = false

    override fun isSpecialization(): Boolean = false

    override fun getQualifiedName(): OCQualifiedName = qualifiedName

    override fun hasProperty(property: OCDeclaratorSymbol.Property): Boolean = false

    override fun dropSubstitution(): OCSymbol = this

    override fun isVariadicTemplate(): Boolean = false

    override fun getTemplateParameters(): List<OCTypeParameterSymbol<OCTypeArgument>> = emptyList()

    override fun isExplicitInstantiation(): Boolean = false

    override fun getRequiredTemplateArgumentsCnt(): Int = 0

    override fun getInitializer(): OCExpressionSymbol? = null

    override fun getTemplateSpecialization(): List<OCTypeArgument> = emptyList()

    //todo why containingClass???
    override fun getParent(): OCSymbolWithQualifiedName = containingClass

    override fun getNameWithParent(context: OCResolveContext): String = parent.name + "::" + name

    override fun getType(): OCType = type

    override fun getInitializerExpression(): OCExpressionSymbol? = null
}
