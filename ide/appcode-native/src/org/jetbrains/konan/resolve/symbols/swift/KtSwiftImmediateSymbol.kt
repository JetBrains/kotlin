package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.swift.symbols.*
import org.jetbrains.konan.resolve.symbols.KtImmediateSymbol
import org.jetbrains.konan.resolve.symbols.KtOCLightSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.Stub

abstract class KtSwiftImmediateSymbol : KtImmediateSymbol, SwiftSymbol {
    @Transient
    private lateinit var file: VirtualFile
    @Transient
    private lateinit var project: Project

    constructor(stub: Stub<*>, file: VirtualFile, project: Project) : super(stub) {
        this.file = file
        this.project = project
    }

    constructor() : super()

    override fun getContainingFile(): VirtualFile = file
    override fun getProject(): Project = project

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean {
        if (!super.deepEqualStep(c, first, second)) return false

        val f = first as KtSwiftImmediateSymbol
        val s = second as KtSwiftImmediateSymbol

        if (!Comparing.equal(f.file, s.file)) return false

        return true
    }

    override fun hashCodeExcludingOffset(): Int = name.hashCode() * 31 + file.hashCode()

    override fun isSameSymbol(symbol: OCSymbol?, project: Project): Boolean {
        return super<KtImmediateSymbol>.isSameSymbol(symbol, project)
               || symbol is KtOCLightSymbol && locateDefinition(project) == symbol.locateDefinition(project)
    }

    override fun init(project: Project?, file: VirtualFile?) {
        this.file = file!!
        this.project = project!!
    }

    override fun getKind(): OCSymbolKind = OCSymbolKind.FOREIGN_ELEMENT

    override fun isGlobal(): Boolean = true

    override fun <T : SwiftSymbolAttribute?> getSwiftAttribute(attributeType: SwiftAttributesInfo.AttributeType<T>): T? =
        swiftAttributes.getAttribute(attributeType)

    override fun hasSwiftDeclarationSpecifier(specifier: SwiftDeclarationSpecifiers): Boolean =
        swiftAttributes.hasDeclarationSpecifier(specifier)

    override fun getSwiftAttributes(): SwiftAttributesInfo = SwiftAttributesInfo.EMPTY //todo???

    override fun getShortObjcName(): String? = name //todo???
    override fun getObjcName(): String? = name //todo???
}