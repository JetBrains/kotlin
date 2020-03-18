package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.SwiftCallableSymbol
import com.jetbrains.swift.symbols.SwiftMemberSymbol
import com.jetbrains.swift.symbols.SwiftParameterSymbol
import org.jetbrains.konan.resolve.symbols.swift.*
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

class KtSwiftSymbolTranslator(val project: Project) : KtFileTranslator<KtSwiftTypeSymbol<*, *>, SwiftMemberSymbol>() {
    override fun translate(moduleDescriptor: ModuleDescriptor, stub: ObjCTopLevel<*>, file: VirtualFile): KtSwiftTypeSymbol<*, *>? {
        return when (stub) {
            is ObjCProtocol -> KtSwiftProtocolSymbol(moduleDescriptor, stub, project, file)
            is ObjCInterface -> when (stub.categoryName) {
                null -> KtSwiftClassSymbol(moduleDescriptor, stub, project, file)
                else -> KtSwiftExtensionSymbol(moduleDescriptor, stub, project, file)
            }
            else -> {
                OCLog.LOG.error("unknown kotlin declaration: " + stub::class)
                null
            }
        }
    }

    override fun translateMember(
        stub: Stub<*>, clazz: KtSwiftTypeSymbol<*, *>, file: VirtualFile, processor: (SwiftMemberSymbol) -> Unit
    ) {
        when (stub) {
            is ObjCMethod -> {
                val isConstructor = stub.swiftName == "init" // works due to the attributes set by Kotlin ObjC export
                when (isConstructor) {
                    true -> KtSwiftInitializerSymbol(stub, file, project, clazz)
                    false -> KtSwiftMethodSymbol(stub, file, project, clazz)
                }.also { method ->
                    val parameters = translateParameters(stub, method, file)
                    val returnType = stub.returnType.convertType(method)
                    method.swiftType = SwiftTypeFactory.getInstance().run {
                        val functionType = createFunctionType(createDomainType(parameters), returnType, false)
                        when (isConstructor || !stub.isInstanceMethod) {
                            true -> functionType
                            false -> createImplicitSelfMethodType(functionType)
                        }
                    }
                }.also(processor)
            }
            is ObjCProperty -> KtSwiftPropertySymbol(stub, project, file, clazz).also { property ->
                property.swiftType = stub.type.convertType(property)
            }.also(processor)
            else -> OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
        }
    }

    private fun translateParameters(
        methodStub: ObjCMethod,
        callableSymbol: SwiftCallableSymbol,
        file: VirtualFile
    ): List<SwiftParameterSymbol> =
        methodStub.parameters.map { parameterStub ->
            KtSwiftParameterSymbol(parameterStub, project, file, callableSymbol).apply {
                swiftType = parameterStub.type.convertType(this@apply)
            }
        }
}