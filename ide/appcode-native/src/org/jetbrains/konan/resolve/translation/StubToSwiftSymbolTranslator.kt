package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.swift.symbols.*
import org.jetbrains.konan.resolve.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.*

class StubToSwiftSymbolTranslator(val project: Project) {
    fun translate(stub: Stub<*>, file: VirtualFile): SwiftSymbol? {
        return when (stub) {
            is ObjCProtocol -> KtSwiftProtocolSymbol(stub, project, file)
            is ObjCInterface -> {
                if (stub.categoryName != null) {
                    KtSwiftExtensionSymbol(stub, project, file)
                } else {
                    KtSwiftClassSymbol(stub, project, file)
                }
            }
            else -> {
                OCLog.LOG.error("unknown kotlin declaration: " + stub::class)
                null
            }
        }
    }

    fun translateMember(stub: Stub<*>, clazz: SwiftTypeSymbol, file: VirtualFile): SwiftMemberSymbol? {
        return when (stub) {
            is ObjCMethod -> KtSwiftMethodSymbol(stub, project, file, clazz).also { method ->
                method.setParameters(translateParameters(stub, method, file))
            }
            is ObjCProperty -> KtSwiftPropertySymbol(stub, project, file, clazz)
            else -> {
                OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
                null
            }
        }
    }

    private fun translateParameters(
        stub: ObjCMethod,
        functionSymbol: SwiftFunctionSymbol,
        file: VirtualFile
    ): List<SwiftParameterSymbol> =
        stub.parameters.map { parameter -> KtSwiftParameterSymbol(parameter, project, file, functionSymbol) }
}