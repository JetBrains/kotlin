package org.jetbrains.konan.resolve.symbols.swift

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MostlySingularMultiMap
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.SwiftGenericParametersInfo
import com.jetbrains.swift.symbols.SwiftMemberSymbol
import com.jetbrains.swift.symbols.SwiftTypeSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

abstract class KtSwiftTypeSymbol<State : KtSwiftTypeSymbol.TypeState, Stub : ObjCClass<*>>
    : KtSwiftLazySymbol<State, Stub>, SwiftTypeSymbol {

    constructor(moduleDescriptor: ModuleDescriptor, stub: Stub, project: Project, file: VirtualFile)
            : super(moduleDescriptor, stub, project, file)

    constructor() : super()

    override val context: SwiftMemberSymbol?
        get() = containingTypeSymbol

    override val swiftType: SwiftType
        get() = SwiftTypeFactory.getInstance().createClassType(this)

    //todo
    override fun getGenericParametersInfo(): SwiftGenericParametersInfo = SwiftGenericParametersInfo.EMPTY

    override val qualifiedName: String
        //todo qualified name!!!
        get() = name

    override val rawMembers: MostlySingularMultiMap<String, SwiftMemberSymbol>
        get() = state?.members ?: MostlySingularMultiMap.emptyMap()

    override fun getContainingTypeSymbol(): SwiftTypeSymbol? = null

    open class TypeState : StubState {
        var members: MostlySingularMultiMap<String, SwiftMemberSymbol>?

        constructor(clazz: KtSwiftTypeSymbol<*, *>, stub: ObjCClass<*>, project: Project) : super(stub) {
            members = createTranslator(project).translateMembers(stub, clazz)
        }

        constructor() : super() {
            members = null
        }
    }
}