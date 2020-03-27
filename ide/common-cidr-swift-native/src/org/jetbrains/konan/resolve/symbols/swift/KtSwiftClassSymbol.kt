package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.nullize
import com.jetbrains.swift.psi.SwiftDeclarationKind
import com.jetbrains.swift.psi.types.SwiftClassType
import com.jetbrains.swift.symbols.SwiftClassSymbol
import com.jetbrains.swift.symbols.SwiftInitializerSymbol
import org.jetbrains.konan.resolve.translation.StubAndProject
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface

class KtSwiftClassSymbol : KtSwiftTypeSymbol<KtSwiftClassSymbol.ClassState, ObjCInterface>, SwiftClassSymbol {
    constructor(stubAndProject: StubAndProject<ObjCInterface>, file: VirtualFile)
            : super(stubAndProject, file)

    constructor() : super()

    override val declarationKind: SwiftDeclarationKind
        get() = SwiftDeclarationKind.classDeclaration

    override fun computeState(stub: ObjCInterface, project: Project): ClassState = ClassState(this, stub, project)

    override val rawSuperTypes: List<SwiftClassType>
        get() = state?.superTypes ?: emptyList()

    //todo [medvedev] ??? also implement SwiftObjcClassSymbol.getDesignatedInitializers
    override fun getDesignatedInitializers(): List<SwiftInitializerSymbol> = emptyList()

    class ClassState : TypeState {
        lateinit var superTypes: List<SwiftClassType>

        constructor(classSymbol: KtSwiftClassSymbol, stub: ObjCInterface, project: Project) : super(classSymbol, stub, project) {
            superTypes = (sequenceOf(stub.superClass) + stub.superProtocols.asSequence())
                .filterNotNull()
                .map { ref -> createClassType(ref, classSymbol) }
                .toList().nullize() ?: emptyList()
        }

        constructor() : super()
    }
}
