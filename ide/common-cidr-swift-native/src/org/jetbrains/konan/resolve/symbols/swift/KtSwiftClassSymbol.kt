package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.types.SwiftClassType
import com.jetbrains.swift.symbols.SwiftClassSymbol
import com.jetbrains.swift.symbols.SwiftInitializerSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface

class KtSwiftClassSymbol : KtSwiftTypeSymbol<KtSwiftClassSymbol.ClassState, ObjCInterface>, SwiftClassSymbol {
    constructor(stub: ObjCInterface, project: Project, file: VirtualFile) : super(stub, project, file)
    constructor() : super()

    override val declarationKind: SwiftDeclarationKind
        get() = SwiftDeclarationKind.classDeclaration

    override fun computeState(stub: ObjCInterface, project: Project): ClassState = ClassState(this, stub, project)

    override val rawSuperTypes: List<SwiftClassType>
        get() = state.superTypes

    //todo [medvedev] ??? also implement SwiftObjcClassSymbol.getDesignatedInitializers
    override fun getDesignatedInitializers(): List<SwiftInitializerSymbol> = emptyList()

    class ClassState : TypeState {
        lateinit var superTypes: List<SwiftClassType>

        constructor(
            classSymbol: KtSwiftClassSymbol,
            stub: ObjCInterface,
            project: Project
        ) : super(classSymbol, stub, project) {
            superTypes = (sequenceOf(stub.superClass) + stub.superProtocols.asSequence())
                .filterNotNull()
                .map { ref -> createClassType(ref, classSymbol) }
                .toList()
        }

        constructor() : super()
    }
}
