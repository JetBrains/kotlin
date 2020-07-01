package org.jetbrains.konan.resolve.symbols.objc

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Processor
import com.intellij.util.containers.MostlySingularMultiMap
import com.intellij.util.containers.nullize
import com.jetbrains.cidr.lang.symbols.OCQualifiedName
import com.jetbrains.cidr.lang.symbols.OCResolveContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.OCVisibility
import com.jetbrains.cidr.lang.symbols.cpp.OCSymbolWithQualifiedName
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCImplementationSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMemberSymbol
import com.jetbrains.cidr.lang.types.OCObjectType
import org.jetbrains.konan.resolve.translation.KtOCSymbolTranslator
import org.jetbrains.konan.resolve.translation.TranslationState
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass

abstract class KtOCClassSymbol<State : KtOCClassSymbol.ClassState, Stub : ObjCClass<*>> : KtOCLazySymbol<State, Stub>, OCClassSymbol {
    private lateinit var qualifiedName: OCQualifiedName

    constructor(translationState: TranslationState<Stub>, file: VirtualFile)
            : super(translationState, file) {
        qualifiedName = OCQualifiedName.interned(translationState.stub.name)
    }

    constructor() : super()

    override fun isGlobal(): Boolean = true

    override fun getProtocolNames(): List<String> = state?.protocolNames ?: emptyList()

    override fun getMembersCount(): Int = state?.members?.size() ?: 0

    @Suppress("UNCHECKED_CAST")
    override fun <T : OCMemberSymbol?> processMembers(
        memberName: String?,
        memberClass: Class<out T>?,
        processor: Processor<in T>
    ): Boolean {
        val members = state?.members ?: return true

        val myProcessor = if (memberClass != null) {
            Processor { member -> !memberClass.isAssignableFrom(member.javaClass) || processor.process(member as T) }
        } else {
            processor as Processor<OCMemberSymbol>
        }

        return if (memberName == null) {
            members.processAllValues(myProcessor)
        } else {
            members.processForKey(memberName, myProcessor)
        }
    }

    override fun getDefinitionSymbol(project: Project): OCClassSymbol? = this

    override fun getInterfaceOrProtocol(project: Project): OCClassSymbol? = this

    override fun getImplementation(project: Project): OCImplementationSymbol? = null

    override fun getResolvedType(context: OCResolveContext, ignoringImports: Boolean): OCObjectType? =
        type.resolve(context, ignoringImports) as? OCObjectType

    override fun getParent(): OCSymbolWithQualifiedName? = null

    override fun getNameWithParent(context: OCResolveContext): String = name

    override fun getVisibility(): OCVisibility? = null

    override fun getQualifiedName(): OCQualifiedName = qualifiedName

    override fun dropSubstitution(): OCSymbol = this

    open class ClassState : StubState {
        val members: MostlySingularMultiMap<String, OCMemberSymbol>?
        lateinit var protocolNames: List<String>

        constructor(clazz: KtOCClassSymbol<*, *>, stub: ObjCClass<*>, project: Project) : super(stub) {
            protocolNames = stub.superProtocols.nullize() ?: emptyList()
            members = KtOCSymbolTranslator.translateMembers(stub, project, clazz)
        }

        constructor() : super() {
            members = null
        }
    }
}