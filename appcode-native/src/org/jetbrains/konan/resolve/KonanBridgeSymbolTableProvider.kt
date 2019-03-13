/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.cidr.CidrLog
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMemberSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbol
import com.jetbrains.cidr.lang.symbols.objc.SelectorPartSymbolImpl
import com.jetbrains.cidr.lang.symbols.symtable.ContextSignature
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.OCSymbolTablesBuildingActivity
import com.jetbrains.cidr.lang.symbols.symtable.SymbolTableProvider
import org.jetbrains.konan.resolve.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile

//todo copied from SwiftBridgeSymbolTableProvider
class KonanBridgeSymbolTableProvider : SymbolTableProvider() {
    public override fun isSource(file: PsiFile): Boolean {
        return file is KonanBridgePsiFile
    }

    override fun isSource(file: VirtualFile, cachedFileType: Lazy<FileType>): Boolean {
        return file is KonanBridgeVirtualFile
    }

    override fun onOutOfCodeBlockModification(project: Project, file: PsiFile?) {
        //nothing here
    }

    override fun isOutOfCodeBlockChange(p0: PsiTreeChangeEventImpl): Boolean = false

    override fun calcTableUsingPSI(file: PsiFile, virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        CidrLog.LOG.error("should not be called for this file: " + file.name)
        return FileSymbolTable(virtualFile, ContextSignature())
    }

    override fun calcTable(virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        virtualFile as KonanBridgeVirtualFile
        val signature = ContextSignature(CLanguageKind.OBJ_C, emptyMap(), emptySet(), emptyList(), false)
        val result = FileSymbolTable(virtualFile, signature)
        val moduleDescriptor = findModuleDescriptor(virtualFile) ?: return result
        val generator = Generator(moduleDescriptor)
        val declarations: List<Stub<*>> = generator.translateModule()
        declarations.forEach {
            translate(it, context.project)?.let { result.append(it) }
        }
        return result
    }

    private fun findModuleDescriptor(virtualFile: KonanBridgeVirtualFile): ModuleDescriptor? {
        //todo implement proper module stuff here
        val baseDir = virtualFile.target.file.baseDir
        val ktSrcRoot = baseDir
            .findChild("KotlinNativeFramework")
            ?.findChild("src")
            ?.findChild("KotlinNativeFrameworkMain")
            ?.findChild("kotlin")
            ?: return null
        val ktVFile = ktSrcRoot.children.first()
        val ktFile = PsiManager.getInstance(virtualFile.project).findFile(ktVFile) as? KtFile ?: return null
        return ktFile.findModuleDescriptor()
    }

    private fun translate(stub: Stub<*>, project: Project): OCSymbol? {
        val clazz = when (stub) {
            is ObjCProtocol -> KotlinOCProtocolSymbol(stub, project)
            is ObjCInterface -> KotlinOCInterfaceSymbol(stub, project)
            else -> {
                OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
                return null
            }
        }
        clazz.setMembers((stub as ObjCClass).members.asSequence().mapNotNull { member -> translateMember(member, clazz, project) })
        return clazz
    }

    private fun translateMember(stub: Stub<*>, clazz: OCClassSymbol, project: Project): OCMemberSymbol? {
        return when (stub) {
            is ObjCMethod -> {
                val method = KotlinOCMethodSymbol(stub, project, clazz)
                method.selectors = translateParameters(stub, project, clazz)
                method
            }
            is ObjCProperty -> KotlinOCPropertySymbol(stub, project, clazz)
            else -> {
                OCLog.LOG.error("unknown kotlin objective-c declaration: " + stub::class)
                null
            }
        }
    }

    private fun translateParameters(stub: ObjCMethod, project: Project, clazz: OCClassSymbol): List<OCMethodSymbol.SelectorPartSymbol> {
        val selectors = stub.selectors
        val parameters = stub.parameters

        return if (selectors.size == 1 && parameters.size == 0) {
            listOf(SelectorPartSymbolImpl(null, selectors[0]))
        } else {
            assert(selectors.size == parameters.size)
            ContainerUtil.zip(parameters, selectors).asSequence().map {
                SelectorPartSymbolImpl(KotlinOCParameterSymbol(it.first, project, clazz), it.second)
            }.toList()
        }
    }


    override fun getItemProviderAndWorkerForAdditionalSymbolLoading(
        project: Project,
        indicator: ProgressIndicator,
        allFiles: Collection<VirtualFile>
    ): OCSymbolTablesBuildingActivity.TaskProvider<*>? {
        return null
    }

    class Generator(moduleDescriptor: ModuleDescriptor) : ObjCExportHeaderGenerator(moduleDescriptor, moduleDescriptor.builtIns, "KNF") {
        override fun reportWarning(text: String) {
            /*todo medvedev*/
        }

        override fun reportWarning(method: FunctionDescriptor, text: String) {
            /*todo medvedev*/
        }
    }
}