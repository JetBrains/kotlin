/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

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
import org.jetbrains.konan.resolve.toOCType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter

class KotlinOCParameterSymbol(
    stub: ObjCParameter,
    project: Project,
    file: VirtualFile,
    private val containingClass: OCClassSymbol
) : KotlinOCWrapperSymbol<ObjCParameter>(stub, project, file), OCDeclaratorSymbol {

    private val myQualifiedName: OCQualifiedName = OCQualifiedName.interned(name)
    private val myType: OCType by stub { type.toOCType(project, containingClass) }

    override fun getKind(): OCSymbolKind = OCSymbolKind.PARAMETER

    override fun isGlobal(): Boolean = false

    override fun hasAttribute(attribute: OCSymbolAttribute): Boolean = false

    override fun getSubstitution(): OCTypeSubstitution = OCTypeSubstitution.ID

    override fun getVisibility(): OCVisibility? = null

    override fun isTemplateSymbol(): Boolean = false

    override fun isSpecialization(): Boolean = false

    override fun getQualifiedName(): OCQualifiedName = myQualifiedName

    override fun hasProperty(property: OCDeclaratorSymbol.Property): Boolean = false

    override fun dropSubstitution(): OCSymbol = this

    override fun isVariadicTemplate(): Boolean = false

    override fun getTemplateParameters(): List<OCTypeParameterSymbol<OCTypeArgument>> = emptyList()

    override fun isExplicitInstantiation(): Boolean = false

    override fun getRequiredTemplateArgumentsCnt(): Int = 0

    override fun getInitializer(): OCExpressionSymbol? = null

    override fun getTemplateSpecialization(): List<OCTypeArgument> = emptyList()

    override fun getParent(): OCSymbolWithQualifiedName = containingClass

    override fun getNameWithParent(context: OCResolveContext): String = parent.name + "::" + name

    override fun getType(): OCType = myType

    override fun getInitializerExpression(): OCExpressionSymbol? = null
}
