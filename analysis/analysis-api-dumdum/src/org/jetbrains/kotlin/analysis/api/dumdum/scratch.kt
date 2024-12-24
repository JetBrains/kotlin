package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileId
import org.jetbrains.kotlin.analysis.api.dumdum.index.inMemoryIndex
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

data class StringFileId(val id: String) : FileId {
    override val fileName: String get() = id
}

fun test1() {
    withWobbler {
        val files = mapOf(
            StringFileId("/src/foo/foo.kt") to """
                    
                    package foo
                    
                    import bar.Bar
                    
                    class Foo {
                        fun foo() { Bar().bar() }
                    }
                    
                    fun foo() { }
                    
                    """.trimIndent(),

            StringFileId("/src/bar/bar.kt") to """

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
            val psiManager = PsiManager.getInstance(project)
            inMemoryIndex(
                files.keys.associateWith { fileId ->
                    val vFile = virtualFile(fileId, KotlinFileType.INSTANCE) {
                        files[fileId]!!.toByteArray()
                    }
                    val psiFile = psiManager.findFile(vFile)!!
                    mapFile(psiFile)
                }
            )
        }

        files.forEach { (fileId, text) ->
            withProject {
                val singleModule = KaSourceModuleImpl(
                    directRegularDependencies = emptyList(),
                    directDependsOnDependencies = emptyList(),
                    directFriendDependencies = emptyList(),
                    contentScope = GlobalSearchScope.allScope(project),
                    targetPlatform = JvmPlatforms.defaultJvmPlatform,
                    project = project,
                    name = "dumdum",
                    languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST),
                )
                val psiManager = PsiManager.getInstance(project)

                val vFile = virtualFile(fileId, KotlinFileType.INSTANCE) {
                    text.toByteArray()
                }
                val psiFile = psiManager.findFile(vFile)!!

                withAnalyzer(
                    index = index,
                    singleModule = singleModule,
                    virtualFileFactory = { id ->
                        virtualFile(id, KotlinFileType.INSTANCE) {
                            files[id]!!.toByteArray()
                        }
                    }
                ) {
                    analyze(psiFile as KtFile) {
                        psiFile.descendantsOfType<KtCallElement>().forEach { call ->
                            val callInfo = (call.resolveToCall() as KaSuccessCallInfo).call as KaCallableMemberCall<*, *>

                            val sig = callInfo.partiallyAppliedSymbol.signature

                            println(sig.callableId)
                            println(sig.symbol.psi)
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

internal class KaSourceModuleImpl(
    override val directRegularDependencies: List<KaModule>,
    override val directDependsOnDependencies: List<KaModule>,
    override val directFriendDependencies: List<KaModule>,
    override val contentScope: GlobalSearchScope,
    override val targetPlatform: TargetPlatform,
    override val project: Project,
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    override val transitiveDependsOnDependencies: List<KaModule> = emptyList(),
) : KaSourceModule {
    @KaExperimentalApi
    override val stableModuleName: String? get() = name

    @KaExperimentalApi
    override val psiRoots: List<PsiFileSystemItem>
        get() = throw UnsupportedOperationException("don't do this")
}
