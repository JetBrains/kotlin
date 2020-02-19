package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.swift.presentation.calculateFastIcon
import com.jetbrains.swift.presentation.calculateFullIcon
import com.jetbrains.swift.presentation.calculateMaybeDeferredFullIcon
import com.jetbrains.swift.psi.types.SwiftContext
import com.jetbrains.swift.symbols.*
import org.jetbrains.konan.resolve.symbols.KtImmediateSymbol
import org.jetbrains.konan.resolve.symbols.KtSwiftSymbolPsiWrapper
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import javax.swing.Icon

abstract class KtSwiftImmediateSymbol : KtImmediateSymbol, SwiftSymbol, SwiftContextInitializable {
    @Transient
    private lateinit var file: VirtualFile
    @Transient
    private lateinit var project: Project

    override val projectFile: SwiftContext
        get() = SwiftContext.of(file, project)

    constructor(stub: Stub<*>, file: VirtualFile, project: Project) : super(stub, stub.swiftName) {
        this.file = file
        this.project = project
        this.objcName = stub.name
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

    override fun init(project: Project, file: VirtualFile) {
        this.file = file
        this.project = project
    }

    final override fun init(projectFile: SwiftContext) {
        init(projectFile.project, requireNotNull(projectFile.virtualFile))
    }

    override fun getKind(): OCSymbolKind = OCSymbolKind.FOREIGN_ELEMENT

    override fun isGlobal(): Boolean = true

    override fun <T : SwiftSymbolAttribute> getSwiftAttribute(type: SwiftAttributesInfo.AttributeType<T>): T? =
            swiftAttributes.getAttribute(type)

    override fun hasSwiftDeclarationSpecifier(declarationSpecifier: SwiftDeclarationSpecifiers): Boolean =
            swiftAttributes.hasDeclarationSpecifier(declarationSpecifier)

    override val swiftAttributes: SwiftAttributesInfo
        get() = publicSwiftAttributes //todo???

    override fun getIcon(project: Project): Icon? = getIcon(project, 0)

    override fun getIcon(project: Project, @Iconable.IconFlags flags: Int): Icon? =
            calculateMaybeDeferredFullIcon(this, null, getProject(), flags)

    override fun getBaseIcon(): Icon? =
            calculateFastIcon(this, null, getProject(), 0)

    override fun computeFullIconNow(symbolElement: PsiElement?, project: Project): Icon? =
            calculateFullIcon(this, symbolElement, project, 0)

    override val shortObjcName: String
        get() = objcName //todo???

    final override lateinit var objcName: String //todo???
        private set

    override fun locateDefinition(project: Project): PsiElement? = doLocateDefinition(project)?.let { KtSwiftSymbolPsiWrapper(it, this) }
}