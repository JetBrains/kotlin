package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.CidrLog.LOG
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.SwiftCallableSymbol
import com.jetbrains.swift.symbols.SwiftMemberSymbol
import com.jetbrains.swift.symbols.SwiftParameterSymbol
import org.jetbrains.konan.resolve.symbols.KtLazySymbol
import org.jetbrains.konan.resolve.symbols.swift.*
import org.jetbrains.kotlin.backend.konan.objcexport.*

object KtSwiftSymbolTranslator : KtFileTranslator<KtSwiftTypeSymbol<*, *>, SwiftMemberSymbol>() {
    override fun translate(
        stubTrace: StubTrace, stubs: Collection<ObjCTopLevel<*>>, file: VirtualFile, destination: MutableList<in KtSwiftTypeSymbol<*, *>>
    ) {
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
            val previousSymbolWithSameName = containingSymbols.putIfAbsent(qualifiedName, symbol)
            LOG.assertTrue(
                previousSymbolWithSameName == null,
                "Two classes with same qualified name ($qualifiedName):\n$previousSymbolWithSameName\n$symbol"
            )

            when (containingSymbol) {
                null -> destination.add(symbol)
                else -> {
                    symbol.containingSymbol = containingSymbol
                    containingSymbol.addContainedSymbol(symbol)
                }
            }

            LOG.assertTrue(symbol.qualifiedName == qualifiedName, "Qualified name does not match input")
        }
        if (!KtLazySymbol.useLazyTranslation()) containingSymbols.values.forEach { it.ensureStateLoaded() }
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

    override fun translateMember(
        stub: Stub<*>, project: Project, file: VirtualFile, containingClass: KtSwiftTypeSymbol<*, *>,
        processor: (SwiftMemberSymbol) -> Unit
    ) {
        when (stub) {
            is ObjCMethod -> {
                val isConstructor = stub.swiftName == "init" // works due to the attributes set by Kotlin ObjC export
                when (isConstructor) {
                    true -> KtSwiftInitializerSymbol(stub, file, project, containingClass)
                    false -> KtSwiftMethodSymbol(stub, file, project, containingClass)
                }.also { method ->
                    val parameters = translateParameters(stub, method, project, file)
                    val returnType = stub.returnType.convertType(method)
                    method.swiftType = SwiftTypeFactory.getInstance().run {
                        val functionType = createFunctionType(createDomainType(parameters), returnType, false)
                        when (isConstructor || !stub.isInstanceMethod) {
                            true -> functionType
                            false -> createFunctionType(createDomainType(createSelfType(containingClass, true)), functionType, false)
                        }
                    }
                }.also(processor)
            }
            is ObjCProperty -> KtSwiftPropertySymbol(stub, project, file, containingClass).also { property ->
                property.swiftType = stub.type.convertType(property)
            }.also(processor)
            else -> OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
        }
    }

    private fun translateParameters(
        methodStub: ObjCMethod, callableSymbol: SwiftCallableSymbol, project: Project, file: VirtualFile
    ): List<SwiftParameterSymbol> =
        methodStub.parameters.map { parameterStub ->
            KtSwiftParameterSymbol(parameterStub, project, file, callableSymbol).apply {
                swiftType = parameterStub.type.convertType(this@apply)
            }
        }
}