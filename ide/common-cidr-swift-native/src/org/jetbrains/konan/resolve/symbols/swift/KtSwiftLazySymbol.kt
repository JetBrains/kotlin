package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.swift.presentation.calculateFastIcon
import com.jetbrains.swift.presentation.calculateFullIcon
import com.jetbrains.swift.presentation.calculateMaybeDeferredFullIcon
import com.jetbrains.swift.psi.types.SwiftContext
import com.jetbrains.swift.symbols.*
import org.jetbrains.konan.resolve.symbols.KtLazySymbol
import org.jetbrains.konan.resolve.translation.KtSwiftSymbolTranslator
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import javax.swing.Icon

abstract class KtSwiftLazySymbol<State : KtLazySymbol.StubState, Stb : ObjCTopLevel<*>>
    : KtLazySymbol<State, Stb>, SwiftSymbol, SwiftContextInitializable {
    @Transient
    private lateinit var file: VirtualFile

    @Transient
    private lateinit var project: Project

    override val projectFile: SwiftContext
        get() = SwiftContext.of(file, project)

    constructor(moduleDescriptor: ModuleDescriptor, stub: Stb, project: Project, file: VirtualFile)
            : super(moduleDescriptor, stub, project, stub.swiftName) {
        this.file = file
        this.project = project
        this.objcName = stub.name
    }

    constructor() : super()

    override fun init(project: Project, file: VirtualFile) {
        this.file = file
        this.project = project
    }

    final override fun init(projectFile: SwiftContext) {
        init(projectFile.project, requireNotNull(projectFile.virtualFile))
    }

    override fun getKind(): OCSymbolKind = OCSymbolKind.FOREIGN_ELEMENT
    override fun isGlobal(): Boolean = true

    override fun getProject(): Project = project
    override fun getContainingFile(): VirtualFile = file

    override fun <T : SwiftSymbolAttribute> getSwiftAttribute(type: SwiftAttributesInfo.AttributeType<T>): T? =
        swiftAttributes.getAttribute(type)

    override fun hasSwiftDeclarationSpecifier(declarationSpecifier: SwiftDeclarationSpecifiers): Boolean =
        swiftAttributes.hasDeclarationSpecifier(declarationSpecifier)

    override fun hashCodeExcludingOffset(): Int = name.hashCode() * 31 + projectFile.hashCode()

    override fun getIcon(project: Project): Icon? = getIcon(project, 0)

    override fun getIcon(project: Project, @Iconable.IconFlags flags: Int): Icon? =
        calculateMaybeDeferredFullIcon(this, null, getProject(), flags)

    override fun getBaseIcon(): Icon? =
        calculateFastIcon(this, null, getProject(), 0)

    override fun computeFullIconNow(symbolElement: PsiElement?, project: Project): Icon? =
        calculateFullIcon(this, symbolElement, project, 0)

    override val swiftAttributes: SwiftAttributesInfo
        get() = publicSwiftAttributes //todo???

    override val shortObjcName: String?
        get() = objcName //todo???

    final override lateinit var objcName: String //todo???
        private set

    companion object {
        @JvmStatic
        protected fun createTranslator(project: Project): KtSwiftSymbolTranslator = KtSwiftSymbolTranslator(project)
    }
}