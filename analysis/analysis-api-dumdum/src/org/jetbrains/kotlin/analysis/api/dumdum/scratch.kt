package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileId
import org.jetbrains.kotlin.analysis.api.dumdum.index.VirtualFileFactory
import org.jetbrains.kotlin.analysis.api.dumdum.index.inMemoryIndex
import org.jetbrains.kotlin.analysis.api.dumdum.index.indexFile
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtFile

fun test1() {
    withWobbler {
        val files = mapOf(
            FileId("/src/foo/foo.kt") to """
                    
                    package foo
                    
                    import bar.Bar
                    
                    class Foo {
                        fun foo() { Bar().bar() }
                    }
                    
                    fun foo() { }
                    
                    """.trimIndent(),

            FileId("/src/bar/bar.kt") to """

                    package bar
                    
                    import foo.Foo
                    import foo.foo 
                    
                    class Bar {
                        fun bar() { Foo().foo() }
                    }
                    
                    fun bar() { foo() }
                    
                    """.trimIndent()
        )
        val index = withProject {
            val virtualFileById = files.mapValues { (fileId, content) ->
                LightVirtualFile(
                    fileId.id.split('/').last(),
                    KotlinFileType.INSTANCE,
                    content
                ).also {
                    it.putUserData(FileIdKey, fileId)
                }
            }

            val psiManager = PsiManager.getInstance(project)
            val psiFileById = virtualFileById.mapValues { (_, virtualFile) -> psiManager.findFile(virtualFile)!! }

            inMemoryIndex(
                psiFileById.flatMap { (fileId, psiFile) ->
                    indexFile(
                        fileId = fileId,
                        file = psiFile,
                        fileBasedIndexExtensions = fileBasedIndexExtensions,
                        stubIndexExtensions = stubIndexExtensions,
                        stubSerializerTable = stubSerializersTable,
                    )
                }
            )
        }

        withProject {
            val virtualFileById = files.mapValues { (fileId, content) ->
                LightVirtualFile(
                    fileId.id.split('/').last(),
                    KotlinFileType.INSTANCE,
                    content
                ).also {
                    it.putUserData(FileIdKey, fileId)
                }
            }
            val psiManager = PsiManager.getInstance(project)
            val psiFileById = virtualFileById.mapValues { (_, virtualFile) -> psiManager.findFile(virtualFile)!! }

            val singleModule = KaSourceModuleImpl(
                directRegularDependencies = emptyList(),
                directDependsOnDependencies = emptyList(),
                directFriendDependencies = emptyList(),
                contentScope = GlobalSearchScope.filesScope(project, virtualFileById.values),
                targetPlatform = JvmPlatforms.defaultJvmPlatform,
                project = project,
                name = "dumdum",
                languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST),
                psiRoots = psiFileById.values.toList()
            )

            val virtualFileFactory = VirtualFileFactory { fileId ->
                virtualFileById[fileId]!!
            }

            withAnalyzer(index, singleModule, virtualFileFactory) {
                for (psiFile in psiFileById) {
                    analyze(psiFile.value as KtFile) {
                        psiFile.value.descendantsOfType<KtCallElement>().forEach { call ->
                            val callInfo = (call.resolveToCall() as KaSuccessCallInfo).call as KaCallableMemberCall<*, *>
                            println(callInfo.partiallyAppliedSymbol.signature.callableId)
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    test1()
}

internal data class KaSourceModuleImpl(
    override val directRegularDependencies: List<KaModule>,
    override val directDependsOnDependencies: List<KaModule>,
    override val directFriendDependencies: List<KaModule>,
    override val contentScope: GlobalSearchScope,
    override val targetPlatform: TargetPlatform,
    override val project: Project,
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    @KaExperimentalApi
    override val psiRoots: List<PsiFileSystemItem>,
    override val transitiveDependsOnDependencies: List<KaModule> = emptyList(),
) : KaSourceModule {
    @KaExperimentalApi
    override val stableModuleName: String? get() = name
}


private inline fun <T> Disposable.use(block: (Disposable) -> T): T =
    try {
        block(this)
    } finally {
        Disposer.dispose(this)
    }
