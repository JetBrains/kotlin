package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.component1
import com.intellij.openapi.util.component2
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.lang.symbols.objc.OCMemberSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbol.SelectorPartSymbol
import com.jetbrains.cidr.lang.symbols.objc.SelectorPartSymbolImpl
import com.jetbrains.cidr.lang.types.OCVoidType
import org.jetbrains.konan.resolve.symbols.objc.*
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

class KtOCSymbolTranslator(val project: Project) : KtFileTranslator<KtOCClassSymbol<*, *>, OCMemberSymbol>() {
    override fun translate(moduleDescriptor: ModuleDescriptor, stub: ObjCTopLevel<*>, file: VirtualFile): KtOCClassSymbol<*, *>? {
        return when (stub) {
            is ObjCProtocol -> KtOCProtocolSymbol(moduleDescriptor, stub, project, file)
            is ObjCInterface -> KtOCInterfaceSymbol(moduleDescriptor, stub, project, file)
            else -> {
                OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
                null
            }
        }
    }

    override fun translateMember(stub: Stub<*>, clazz: KtOCClassSymbol<*, *>, file: VirtualFile, processor: (OCMemberSymbol) -> Unit) {
        when (stub) {
            is ObjCMethod -> KtOCMethodSymbol(stub, project, file, clazz, translateParameters(stub, clazz, file)).also(processor)
            is ObjCProperty -> KtOCPropertySymbol(stub, project, file, clazz).also(processor).also { property ->
                property.getterName.let {
                    KtOCMethodSymbol(property, stub, it, property.type, file, clazz, listOf(SelectorPartSymbolImpl(null, it)))
                }.also(processor)

                if (!property.isReadonly) {
                    property.setterName.let {
                        val selectors = listOf(SelectorPartSymbolImpl(KtOCParameterSymbol(property, stub, file, clazz), it))
                        KtOCMethodSymbol(property, stub, it, OCVoidType.instance(), file, clazz, selectors)
                    }.also(processor)
                }
            }
            else -> OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
        }
    }

    private fun translateParameters(stub: ObjCMethod, clazz: KtOCClassSymbol<*, *>, file: VirtualFile): List<SelectorPartSymbol> {
        val selectors = stub.selectors
        val parameters = stub.parameters

        return if (selectors.size == 1 && parameters.isEmpty()) {
            listOf(SelectorPartSymbolImpl(null, selectors[0]))
        } else {
            assert(selectors.size == parameters.size)
            ContainerUtil.zip(parameters, selectors).map { (param, selector) ->
                SelectorPartSymbolImpl(KtOCParameterSymbol(param, project, file, clazz), selector)
            }
        }
    }
}