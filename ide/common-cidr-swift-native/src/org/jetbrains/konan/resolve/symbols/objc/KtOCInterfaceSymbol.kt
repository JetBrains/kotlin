package org.jetbrains.konan.resolve.symbols.objc

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.nullize
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.cidr.lang.symbols.OCTypeParameterSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCGenericParameterSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCInterfaceSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCInterfaceSymbolImpl
import com.jetbrains.cidr.lang.types.OCReferenceType
import com.jetbrains.cidr.lang.types.OCType
import com.jetbrains.cidr.lang.types.OCTypeArgument
import com.jetbrains.cidr.lang.types.visitors.OCTypeSubstitution
import org.jetbrains.konan.resolve.translation.TranslationState
import org.jetbrains.konan.resolve.translation.createSuperType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface

class KtOCInterfaceSymbol : KtOCClassSymbol<KtOCInterfaceSymbol.InterfaceState, ObjCInterface>, OCInterfaceSymbol {
    private var categoryName: String?

    constructor(translationState: TranslationState<ObjCInterface>, file: VirtualFile)
            : super(translationState, file) {
        this.categoryName = translationState.stub.categoryName
    }

    constructor() : super() {
        this.categoryName = null
    }

    override fun getKind(): OCSymbolKind = OCSymbolKind.INTERFACE

    override fun getInterface(project: Project): OCInterfaceSymbol? = this

    override fun getSubstitution(): OCTypeSubstitution = OCTypeSubstitution.ID

    override fun isTemplateSymbol(): Boolean = state?.isTemplateSymbol ?: false

    override fun isVariadicTemplate(): Boolean = false

    //todo implement generics
    override fun getTemplateParameters(): List<OCTypeParameterSymbol<OCTypeArgument>> = emptyList()

    override fun isSpecialization(): Boolean = false

    override fun isExplicitInstantiation(): Boolean = false

    override fun getRequiredTemplateArgumentsCnt(): Int = templateParameters.size

    override fun getTemplateSpecialization(): List<OCTypeArgument>? = null

    override fun getCategoryName(): String? = categoryName

    override fun getSuperType(): OCReferenceType = state?.superType ?: OCReferenceType.fromText("")

    //todo implement generics
    override fun getGenericParameters(): List<OCGenericParameterSymbol> = emptyList()

    override fun getType(): OCType = OCInterfaceSymbolImpl.getInterfaceTypeImpl(this)

    override fun computeState(stub: ObjCInterface, project: Project): InterfaceState = InterfaceState(this, stub, project)

    class InterfaceState : ClassState {
        lateinit var superType: OCReferenceType
        val isTemplateSymbol: Boolean

        constructor(clazz: KtOCInterfaceSymbol, stub: ObjCInterface, project: Project) : super(clazz, stub, project) {
            this.superType = createSuperType(stub.superClass, stub.superProtocols.nullize())
            this.isTemplateSymbol = stub.generics.isNotEmpty()
        }

        constructor() : super() {
            this.isTemplateSymbol = false
        }
    }
}