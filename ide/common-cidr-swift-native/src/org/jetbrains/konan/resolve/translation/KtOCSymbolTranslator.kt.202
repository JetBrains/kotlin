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
import org.jetbrains.konan.resolve.symbols.KtLazySymbol
import org.jetbrains.konan.resolve.symbols.objc.*
import org.jetbrains.kotlin.backend.konan.objcexport.*

object KtOCSymbolTranslator : KtFileTranslator<KtOCClassSymbol<*, *>, OCMemberSymbol>() {
    override fun translate(
        stubTrace: StubTrace, stubs: Collection<ObjCTopLevel<*>>, file: VirtualFile, destination: MutableList<in KtOCClassSymbol<*, *>>
    ) {
        val allowLazy = KtLazySymbol.useLazyTranslation()
        stubs.mapNotNullTo(destination) { translate(stubTrace, it, file)?.apply { if (!allowLazy) ensureStateLoaded() } }
    }

    private fun translate(stubTrace: StubTrace, stub: ObjCTopLevel<*>, file: VirtualFile): KtOCClassSymbol<*, *>? {
        return when (stub) {
            is ObjCProtocol -> KtOCProtocolSymbol(TranslationState(stubTrace, stub), file)
            is ObjCInterface -> KtOCInterfaceSymbol(TranslationState(stubTrace, stub), file)
            else -> {
                OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
                null
            }
        }
    }

    override fun translateMember(
        stub: Stub<*>, project: Project, file: VirtualFile, containingClass: KtOCClassSymbol<*, *>,
        processor: (OCMemberSymbol) -> Unit
    ) {
        when (stub) {
            is ObjCMethod -> KtOCMethodSymbol(
                stub, project, file, containingClass, translateParameters(stub, project, file, containingClass)
            ).also(processor)
            is ObjCProperty -> KtOCPropertySymbol(stub, project, file, containingClass).also(processor).also { property ->
                property.getterName.let {
                    KtOCMethodSymbol(property, stub, it, property.type, file, containingClass, listOf(SelectorPartSymbolImpl(null, it)))
                }.also(processor)

                if (!property.isReadonly) {
                    property.setterName.let {
                        val selectors = listOf(SelectorPartSymbolImpl(KtOCParameterSymbol(property, stub, file, containingClass), it))
                        KtOCMethodSymbol(property, stub, it, OCVoidType.instance(), file, containingClass, selectors)
                    }.also(processor)
                }
            }
            else -> OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
        }
    }

    private fun translateParameters(
        stub: ObjCMethod, project: Project, file: VirtualFile, containingClass: KtOCClassSymbol<*, *>
    ): List<SelectorPartSymbol> {
        val selectors = stub.selectors
        val parameters = stub.parameters

        return if (selectors.size == 1 && parameters.isEmpty()) {
            listOf(SelectorPartSymbolImpl(null, selectors[0]))
        } else {
            assert(selectors.size == parameters.size)
            ContainerUtil.zip(parameters, selectors).map { (param, selector) ->
                SelectorPartSymbolImpl(KtOCParameterSymbol(param, project, file, containingClass), selector)
            }
        }
    }
}