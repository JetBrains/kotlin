package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMemberSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbol
import com.jetbrains.cidr.lang.symbols.objc.SelectorPartSymbolImpl
import org.jetbrains.konan.resolve.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.*

class StubToSymbolTranslator(val project: Project) {
    fun translate(stub: Stub<*>, file: VirtualFile): OCSymbol? {
        return when (stub) {
            is ObjCProtocol -> KtOCProtocolSymbol(stub, project, file)
            is ObjCInterface -> KtOCInterfaceSymbol(stub, project, file)
            else -> {
                OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
                return null
            }
        }
    }

    fun translateMember(stub: Stub<*>, clazz: OCClassSymbol, file: VirtualFile): OCMemberSymbol? {
        return when (stub) {
            is ObjCMethod -> {
                val method = KtOCMethodSymbol(stub, project, file, clazz)
                method.selectors = translateParameters(stub, clazz, file)
                method
            }
            is ObjCProperty -> KtOCPropertySymbol(stub, project, file, clazz)
            else -> {
                OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
                null
            }
        }
    }

    private fun translateParameters(stub: ObjCMethod, clazz: OCClassSymbol, file: VirtualFile): List<OCMethodSymbol.SelectorPartSymbol> {
        val selectors = stub.selectors
        val parameters = stub.parameters

        return if (selectors.size == 1 && parameters.isEmpty()) {
            listOf(SelectorPartSymbolImpl(null, selectors[0]))
        } else {
            assert(selectors.size == parameters.size)
            ContainerUtil.zip(parameters, selectors).asSequence().map {
                SelectorPartSymbolImpl(KtOCParameterSymbol(it.first, project, file, clazz), it.second)
            }.toList()
        }
    }
}