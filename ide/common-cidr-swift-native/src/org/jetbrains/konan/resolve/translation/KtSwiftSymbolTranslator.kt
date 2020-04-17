package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.CidrLog.LOG
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.SwiftCallableSymbol
import com.jetbrains.swift.symbols.SwiftMemberSymbol
import com.jetbrains.swift.symbols.SwiftParameterSymbol
import org.jetbrains.konan.resolve.symbols.swift.*
import org.jetbrains.kotlin.backend.konan.objcexport.*

class KtSwiftSymbolTranslator(val project: Project) : KtFileTranslator<KtSwiftTypeSymbol<*, *>, SwiftMemberSymbol>() {
    override fun translate(stubTrace: StubTrace, stubs: Collection<ObjCTopLevel<*>>, file: VirtualFile): List<KtSwiftTypeSymbol<*, *>> {
        val topLevelSymbols = ArrayList<KtSwiftTypeSymbol<*, *>>(stubs.size)
        val containingSymbols = HashMap<String, KtSwiftTypeSymbol<*, *>>(stubs.size)
        for (stub in stubs) {
            val qualifiedName = stub.swiftName
            val (containingName, name) = qualifiedName.run {
                when (val i = lastIndexOf('.')) {
                    -1 -> null to this
                    else -> substring(0, i) to substring(i + 1)
                }
            }

            val containingSymbol = containingName?.let { containingSymbols[it] }
            LOG.assertTrue(containingName == null || containingSymbol != null, "Containing class expected to appear before contained class")

            val symbol = translate(stubTrace, stub, containingSymbol?.let { name } ?: qualifiedName, file) ?: continue
            containingSymbols.putIfAbsent(qualifiedName, symbol)

            val symbols: MutableList<in KtSwiftTypeSymbol<*, *>> = containingSymbol?.let {
                symbol.containingSymbol = it
                it.mutableContainedSymbols()
            } ?: topLevelSymbols
            symbols.add(symbol)

            LOG.assertTrue(symbol.qualifiedName == qualifiedName, "Qualified name does not match input")
        }
        return topLevelSymbols
    }

    private fun translate(stubTrace: StubTrace, stub: ObjCTopLevel<*>, swiftName: String, file: VirtualFile): KtSwiftTypeSymbol<*, *>? =
        when (stub) {
            is ObjCProtocol -> KtSwiftProtocolSymbol(TranslationState(stubTrace, stub), swiftName, file)
            is ObjCInterface -> when (stub.categoryName) {
                null -> KtSwiftClassSymbol(TranslationState(stubTrace, stub), swiftName, file)
                else -> KtSwiftExtensionSymbol(TranslationState(stubTrace, stub), swiftName, file)
            }
            else -> {
                OCLog.LOG.error("unknown kotlin declaration: " + stub::class)
                null
            }
        }

    override fun translateMember(stub: Stub<*>, clazz: KtSwiftTypeSymbol<*, *>, file: VirtualFile, processor: (SwiftMemberSymbol) -> Unit) {
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