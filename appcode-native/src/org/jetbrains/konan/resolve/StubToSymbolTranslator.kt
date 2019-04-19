package org.jetbrains.konan.resolve

import com.intellij.openapi.project.Project
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
    fun translate(stub: Stub<*>): OCSymbol? {
        val clazz = when (stub) {
            is ObjCProtocol -> KotlinOCProtocolSymbol(stub, project)
            is ObjCInterface -> KotlinOCInterfaceSymbol(stub, project)
            else -> {
                OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
                return null
            }
        }
        clazz.setMembers((stub as ObjCClass).members.asSequence().mapNotNull { member -> translateMember(member, clazz) })
        return clazz
    }

    private fun translateMember(stub: Stub<*>, clazz: OCClassSymbol): OCMemberSymbol? {
        return when (stub) {
            is ObjCMethod -> {
                val method = KotlinOCMethodSymbol(stub, project, clazz)
                method.selectors = translateParameters(stub, clazz)
                method
            }
            is ObjCProperty -> KotlinOCPropertySymbol(stub, project, clazz)
            else -> {
                OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
                null
            }
        }
    }

    private fun translateParameters(stub: ObjCMethod, clazz: OCClassSymbol): List<OCMethodSymbol.SelectorPartSymbol> {
        val selectors = stub.selectors
        val parameters = stub.parameters

        return if (selectors.size == 1 && parameters.isEmpty()) {
            listOf(SelectorPartSymbolImpl(null, selectors[0]))
        } else {
            assert(selectors.size == parameters.size)
            ContainerUtil.zip(parameters, selectors).asSequence().map {
                SelectorPartSymbolImpl(KotlinOCParameterSymbol(it.first, project, clazz), it.second)
            }.toList()
        }
    }
}