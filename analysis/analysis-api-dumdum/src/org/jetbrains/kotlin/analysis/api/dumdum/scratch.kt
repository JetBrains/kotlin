package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.dumdum.filesystem.FileReader
import org.jetbrains.kotlin.analysis.api.dumdum.filesystem.WobblerVirtualFile
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

        val fileReader = FileReader { files[it]!!.toByteArray() }

        val index = withProject {
            val psiManager = PsiManager.getInstance(project)
            inMemoryIndex(
                files.flatMap { (fileId, str) ->
                    val vFile = WobblerVirtualFile(fileReader, fileId, KotlinFileType.INSTANCE)
                    val psiFile = psiManager.findFile(vFile)!!
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
            val psiManager = PsiManager.getInstance(project)
            val virtualFiles = files.keys.map { fileId ->
                WobblerVirtualFile(fileReader, fileId, KotlinFileType.INSTANCE)
            }.toSet()

            val psiFiles = virtualFiles.mapNotNull(psiManager::findFile)

            val singleModule = KaSourceModuleImpl(
                directRegularDependencies = emptyList(),
                directDependsOnDependencies = emptyList(),
                directFriendDependencies = emptyList(),
                contentScope = GlobalSearchScope.filesScope(project, virtualFiles),
                targetPlatform = JvmPlatforms.defaultJvmPlatform,
                project = project,
                name = "dumdum",
                languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST),
                psiRoots = psiFiles
            )

            val virtualFileFactory = VirtualFileFactory { fileId ->
                WobblerVirtualFile(fileReader, fileId, KotlinFileType.INSTANCE)
            }

            withAnalyzer(index, singleModule, virtualFileFactory) {
                for (psiFile in psiFiles) {
                    analyze(psiFile as KtFile) {
                        psiFile.descendantsOfType<KtCallElement>().forEach { call ->
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
