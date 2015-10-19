/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.frontend.di.createLazyResolveSession
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import java.io.File
import java.net.URL

public class BuiltInsReferenceResolver(val project: Project, val startupManager: StartupManager) {

    @Volatile private var moduleDescriptor: ModuleDescriptor? = null

    @Volatile public var builtInsSources: Set<KtFile>? = null
        private set

    @Volatile private var builtinsPackageFragment: PackageFragmentDescriptor? = null

    init {
        startupManager.runWhenProjectIsInitialized { initialize() }
    }

    private fun initialize() {
        assert(moduleDescriptor == null) { "Attempt to initialize twice" }

        val jetBuiltInsFiles = getJetBuiltInsFiles()

        runReadAction {
            val newModuleContext = ContextForNewModule(project, Name.special("<built-ins resolver module>"), TargetPlatform.Default)
            newModuleContext.setDependencies(newModuleContext.module)

            val declarationFactory = FileBasedDeclarationProviderFactory(newModuleContext.storageManager, jetBuiltInsFiles)

            val resolveSession = createLazyResolveSession(
                    newModuleContext,
                    declarationFactory, BindingTraceContext(),
                    TargetPlatform.Default)

            newModuleContext.initializeModuleContents(resolveSession.getPackageFragmentProvider())

            val packageView = newModuleContext.module.getPackage(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME)
            val fragments = packageView.fragments

            this@BuiltInsReferenceResolver.moduleDescriptor = newModuleContext.module
            builtinsPackageFragment = fragments.single()
            builtInsSources = jetBuiltInsFiles
        }
    }

    private fun getJetBuiltInsFiles(): Set<KtFile> {
        return getBuiltInsDirUrls().flatMapTo(hashSetOf<KtFile>()) { getBuiltInSourceFiles(it) }
    }

    private fun getBuiltInSourceFiles(url: URL): Set<KtFile> {
        val fromUrl = VfsUtilCore.convertFromUrl(url)
        val vf = VirtualFileManager.getInstance().findFileByUrl(fromUrl)
        assert(vf != null) { "Virtual file not found by URL: $url" }

        val psiDirectory = PsiManager.getInstance(project).findDirectory(vf!!)
        assert(psiDirectory != null) { "No PsiDirectory for $vf" }
        return psiDirectory!!.getFiles().filterIsInstance<KtFile>().toHashSet()
    }

    private fun findCurrentDescriptorForMember(originalDescriptor: MemberDescriptor): DeclarationDescriptor? {
        val containingDeclaration = findCurrentDescriptor(originalDescriptor.getContainingDeclaration())
        val memberScope = getMemberScope(containingDeclaration) ?: return null

        val renderedOriginal = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(originalDescriptor)
        val descriptors = if (originalDescriptor is ConstructorDescriptor && containingDeclaration is ClassDescriptor) {
            containingDeclaration.getConstructors()
        }
        else {
            memberScope.getAllDescriptors()
        }

        return descriptors.firstOrNull { renderedOriginal == DescriptorRenderer.FQ_NAMES_IN_TYPES.render(it) }
    }

    private fun findCurrentDescriptor(originalDescriptor: DeclarationDescriptor): DeclarationDescriptor? = when(originalDescriptor) {
        is ClassDescriptor -> moduleDescriptor!!.findClassAcrossModuleDependencies(originalDescriptor.classId)

        is PackageFragmentDescriptor -> {
            if (KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME == originalDescriptor.fqName)
                builtinsPackageFragment
            else
                null
        }

        is MemberDescriptor -> findCurrentDescriptorForMember(originalDescriptor)

        else -> null
    }

    companion object {
        private val BUILT_INS_COMPILABLE_SRC_DIR = File("core/builtins/src", KotlinBuiltIns.BUILT_INS_PACKAGE_NAME.asString())

        private val builtInDirUrls = getBuiltInsDirUrls().map { VfsUtilCore.convertFromUrl(it) }

        public fun getInstance(project: Project): BuiltInsReferenceResolver =
            ServiceManager.getService(project, javaClass<BuiltInsReferenceResolver>())

        private fun isFromBuiltinModule(originalDescriptor: DeclarationDescriptor): Boolean {
            // TODO This is optimization only
            // It should be rewritten by checking declarationDescriptor.getSource(), when the latter returns something non-trivial for builtins.
            val containingModule = originalDescriptor.module
            return containingModule.builtIns.builtInsModule == containingModule
        }

        public fun resolveBuiltInSymbol(project: Project, declarationDescriptor: DeclarationDescriptor): PsiElement? {
            if (!isFromBuiltinModule(declarationDescriptor)) {
                return null
            }

            val resolver = getInstance(project)
            if (resolver.moduleDescriptor == null) {
                return null
            }

            val descriptor = resolver.findCurrentDescriptor(declarationDescriptor)
            if (descriptor != null) {
                return DescriptorToSourceUtils.getSourceFromDescriptor(descriptor)
            }
            return null
        }

        public fun isFromBuiltIns(element: PsiElement): Boolean {
            val url = element.getContainingFile()?.getVirtualFile()?.getUrl()
            return url != null && VfsUtilCore.isUnder(url, builtInDirUrls)
        }

        private fun getMemberScope(parent: DeclarationDescriptor?): KtScope? = when(parent) {
            is ClassDescriptor -> parent.getDefaultType().getMemberScope()
            is PackageFragmentDescriptor -> parent.getMemberScope()
            else -> null
        }

        public fun getBuiltInsDirUrls(): Set<URL> {
            val defaultBuiltIns = LightClassUtil.builtInsDirUrl
            // In production, the above URL is enough as it contains sources for both native and compilable built-ins
            // (it's simply the "kotlin" directory in kotlin-plugin.jar)
            // But in tests, sources of built-ins are not added to the classpath automatically, so we manually specify URLs for both:
            // LightClassUtil.getBuiltInsDirUrl() does so for native built-ins and the code below for compilable built-ins

            if (ApplicationManager.getApplication().isUnitTestMode) {
                return setOf(defaultBuiltIns, BUILT_INS_COMPILABLE_SRC_DIR.toURI().toURL())
            }
            return setOf(defaultBuiltIns)
        }

        public fun refreshBuiltIns() {
            getBuiltInsDirUrls().forEach { url ->
                val fromUrl = VfsUtilCore.convertFromUrl(url)
                val vf = VirtualFileManager.getInstance().findFileByUrl(fromUrl)
                assert(vf != null) { "Virtual file not found by URL: $url" }

                // Refreshing VFS: in case the plugin jar was updated, the caches may hold the old value
                vf!!.getChildren()
                vf.refresh(true, true)
                PathUtil.getLocalFile(vf).refresh(true, true)
            }
        }
    }
}
