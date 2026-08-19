/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm

import com.intellij.core.CoreJavaFileManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.compileJavaFiles
import org.jetbrains.kotlin.test.testFramework.disposeRootDisposable
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.io.File

class KotlinCliJavaFileManagerTest {
    private val testRootDisposable: Disposable = Disposer.newDisposable()
    private var javaFilesDir: File = KtTestUtil.tmpDir("java-file-manager-test")
    private val project: Project = createProject()

    @AfterEach
    fun tearDown() {
        javaFilesDir.deleteRecursively()
        disposeRootDisposable(testRootDisposable)
    }

    @Test
    fun testCommon() {
        val manager = configureManager(
            """
            package foo;
            
            public class TopLevel {
                public class Inner {
                       public class Inner {}
                }
            }
            """.trimIndent(),
            "TopLevel"
        )

        assertCanFind(manager, "foo", "TopLevel")
        assertCanFind(manager, "foo", "TopLevel.Inner")
        assertCanFind(manager, "foo", "TopLevel.Inner.Inner")

        assertCannotFind(manager, "foo", "TopLevel\$Inner.Inner")
        assertCannotFind(manager, "foo", "TopLevel.Inner\$Inner")
        assertCannotFind(manager, "foo", "TopLevel.Inner.Inner.Inner")
    }

    @Test
    fun testInnerClassesWithDollars() {
        val manager = configureManager(
            "package foo;\n\n" +
                    "public class TopLevel {\n" +
                    "public class I\$nner {\n" +
                    "   public class I\$nner{}\n" +
                    "   public class \$Inner{}\n" +
                    "   public class In\$ne\$r\${}\n" +
                    "   public class Inner\$\${}\n" +
                    "   public class \$\$\$\$\${}\n" +
                    "}\n" +
                    "public class Inner\$ {\n" +
                    "   public class I\$nner{}\n" +
                    "   public class \$Inner{}\n" +
                    "   public class In\$ne\$r\${}\n" +
                    "   public class Inner\$\${}\n" +
                    "   public class \$\$\$\$\${}\n" +
                    "}\n" +
                    "public class In\$ner\$\$ {\n" +
                    "   public class I\$nner{}\n" +
                    "   public class \$Inner{}\n" +
                    "   public class In\$ne\$r\${}\n" +
                    "   public class Inner\$\${}\n" +
                    "   public class \$\$\$\$\${}\n" +
                    "}\n" +
                    "\n" +
                    "}", "TopLevel")

        assertCanFind(manager, "foo", "TopLevel")

        assertCanFind(manager, "foo", "TopLevel.I\$nner")
        assertCanFind(manager, "foo", "TopLevel.I\$nner.I\$nner")
        assertCanFind(manager, "foo", "TopLevel.I\$nner.\$Inner")
        assertCanFind(manager, "foo", "TopLevel.I\$nner.In\$ne\$r\$")
        assertCanFind(manager, "foo", "TopLevel.I\$nner.Inner\$\$")
        assertCanFind(manager, "foo", "TopLevel.I\$nner.\$\$\$\$\$")

        assertCannotFind(manager, "foo", "TopLevel.I.nner.\$\$\$\$\$")

        assertCanFind(manager, "foo", "TopLevel.Inner\$")
        assertCanFind(manager, "foo", "TopLevel.Inner\$.I\$nner")
        assertCanFind(manager, "foo", "TopLevel.Inner\$.\$Inner")
        assertCanFind(manager, "foo", "TopLevel.Inner\$.In\$ne\$r\$")
        assertCanFind(manager, "foo", "TopLevel.Inner\$.Inner\$\$")
        assertCanFind(manager, "foo", "TopLevel.Inner\$.\$\$\$\$\$")

        assertCannotFind(manager, "foo", "TopLevel.Inner..\$\$\$\$\$")

        assertCanFind(manager, "foo", "TopLevel.In\$ner\$\$")
        assertCanFind(manager, "foo", "TopLevel.In\$ner\$\$.I\$nner")
        assertCanFind(manager, "foo", "TopLevel.In\$ner\$\$.\$Inner")
        assertCanFind(manager, "foo", "TopLevel.In\$ner\$\$.In\$ne\$r\$")
        assertCanFind(manager, "foo", "TopLevel.In\$ner\$\$.Inner\$\$")
        assertCanFind(manager, "foo", "TopLevel.In\$ner\$\$.\$\$\$\$\$")

        assertCannotFind(manager, "foo", "TopLevel.In.ner\$\$.\$\$\$\$\$")
    }

    @Test
    fun testTopLevelClassesWithDollars() {
        val inTheMiddle = configureManager("package foo;\n\n public class Top\$Level {}", "Top\$Level")
        assertCanFind(inTheMiddle, "foo", "Top\$Level")

        val doubleAtTheEnd = configureManager("package foo;\n\n public class TopLevel\$\$ {}", "TopLevel\$\$")
        assertCanFind(doubleAtTheEnd, "foo", "TopLevel\$\$")

        val multiple = configureManager("package foo;\n\n public class Top\$Lev\$el\$ {}", "Top\$Lev\$el\$")
        assertCanFind(multiple, "foo", "Top\$Lev\$el\$")
        assertCannotFind(multiple, "foo", "Top.Lev\$el\$")

        val twoBucks = configureManager("package foo;\n\n public class \$\$ {}", "\$\$")
        assertCanFind(twoBucks, "foo", "\$\$")
    }

    @Test
    fun testTopLevelClassWithDollarsAndInners() {
        val manager = configureManager(
            "package foo;\n\n" + "public class Top\$Level\$\$ {\n" +
                    "public class I\$nner {" + "   public class I\$nner{}" + "   public class In\$ne\$r\${}" + "   public class Inner\$\$\$\$\${}" + "   public class \$Inner{}" + "   public class \${}" + "   public class \$\$\$\$\${}" + "}\n" + "public class Inner {" + "   public class Inner{}" + "}\n" + "\n" + "}",
            "Top\$Level\$\$"
        )

        assertCanFind(manager, "foo", "Top\$Level\$\$")

        assertCanFind(manager, "foo", "Top\$Level\$\$.Inner")
        assertCanFind(manager, "foo", "Top\$Level\$\$.Inner.Inner")

        assertCanFind(manager, "foo", "Top\$Level\$\$.I\$nner")
        assertCanFind(manager, "foo", "Top\$Level\$\$.I\$nner.I\$nner")
        assertCanFind(manager, "foo", "Top\$Level\$\$.I\$nner.In\$ne\$r\$")
        assertCanFind(manager, "foo", "Top\$Level\$\$.I\$nner.Inner\$\$\$\$\$")
        assertCanFind(manager, "foo", "Top\$Level\$\$.I\$nner.\$Inner")
        assertCanFind(manager, "foo", "Top\$Level\$\$.I\$nner.\$")
        assertCanFind(manager, "foo", "Top\$Level\$\$.I\$nner.\$\$\$\$\$")

        assertCannotFind(manager, "foo", "Top.Level\$\$.I\$nner.\$\$\$\$\$")
    }

    @Test
    fun testDoNotThrowOnMalformedInput() {
        val fileWithEmptyName = configureManager("package foo;\n\n public class Top\$Level {}", "")
        val allScope = GlobalSearchScope.allScope(project)
        fileWithEmptyName.findClass("foo.", allScope)
        fileWithEmptyName.findClass(".", allScope)
        fileWithEmptyName.findClass("..", allScope)
        fileWithEmptyName.findClass(".foo", allScope)
    }

    @Test
    fun testSeveralClassesInOneFile() {
        val manager = configureManager("package foo;\n\n" + "public class One {}\n" + "class Two {}\n" + "class Three {}", "One")

        assertCanFind(manager, "foo", "One")

        //NOTE: this is unsupported
        assertCannotFind(manager, "foo", "Two")
        assertCannotFind(manager, "foo", "Three")
    }

    @Test
    fun testScopeCheck() {
        val manager = configureManager("package foo;\n\n" + "public class Test {}\n", "Test")

        assertNotNull(manager.findClass("foo.Test", GlobalSearchScope.allScope(project))) { "Should find class in all scope" }
        assertNull(manager.findClass("foo.Test", GlobalSearchScope.EMPTY_SCOPE)) { "Should not find class in empty scope" }
    }

    /**
     * A reference recorded in a class file is bound to the classpath that class file was compiled against, which is
     * wider than the scope the class is requested from, so it is resolved in the all-scope instead (KT-17897).
     *
     * `p.Ref` is read through a scope holding the output directory alone — the shape of a build that hands the
     * previous output and the libraries to the compiler as separate roots — while the `p.Lib` its signature names
     * lies in the library. Were the reference resolved in the search scope, the return type of `Ref.get()` would
     * silently become an unresolved `p.Lib`.
     *
     * The same for the binary [org.jetbrains.kotlin.load.java.JavaClassFinder] of java-direct:
     * `JavaClassFinderOverBinaryIndexTest.testACrossReferenceIsResolvedOutsideTheVisibleClasspath`.
     */
    @Test
    fun testBinaryCrossReferenceIsResolvedOutsideTheSearchScope() {
        val libraryClasses = compileJava("lib", "p/Lib.java" to "package p; public class Lib {}")
        val outputClasses = compileJava(
            "out",
            "p/Ref.java" to "package p; public class Ref { public Lib get() { return null; } }",
            classpath = libraryClasses,
        )
        val manager = configureManager(binaryRoot(outputClasses), binaryRoot(libraryClasses))
        val outputOnly = scopeOf(outputClasses)

        assertNull(manager.findClass(LIB_ID, outputOnly)) { "p.Lib lies in the library, which this scope does not hold" }

        val ref = checkNotNull(manager.findClass(REF_ID, outputOnly)) { "p.Ref lies in the output directory" }
        val returnType = ref.methods.single { it.name.asString() == "get" }.returnType as JavaClassifierType
        assertEquals(
            LIB_ID.asSingleFqName(), (returnType.classifier as? JavaClass)?.fqName,
            "the return type of Ref.get() is resolved although p.Lib is outside the scope Ref itself was read through"
        )
    }

    /** Compiles [sources], given as pairs of a path relative to the source root and its text, into `$name` of [javaFilesDir]. */
    private fun compileJava(name: String, vararg sources: Pair<String, String>, classpath: File? = null): File {
        val sourceFiles = sources.map { source ->
            File(javaFilesDir, "$name-src/${source.first}").apply {
                parentFile.mkdirs()
                writeText(source.second)
            }
        }
        val destination = File(javaFilesDir, name).apply { mkdirs() }
        val options = listOfNotNull("-d", destination.path, classpath?.let { "-classpath" }, classpath?.path)
        compileJavaFiles(sourceFiles, options).assertSuccessful()
        return destination
    }

    private fun binaryRoot(classesDirectory: File): JavaRoot =
        JavaRoot(virtualFile(classesDirectory), JavaRoot.RootType.BINARY)

    /** The scope of the class files under [classesDirectory], compared by path: the file system hands out a new [VirtualFile] per lookup. */
    private fun scopeOf(classesDirectory: File): GlobalSearchScope {
        val rootPath = virtualFile(classesDirectory).path + "/"
        return object : GlobalSearchScope(project) {
            override fun contains(file: VirtualFile): Boolean = file.path.startsWith(rootPath)
            override fun isSearchInModuleContent(aModule: Module): Boolean = true
            override fun isSearchInLibraries(): Boolean = true
        }
    }

    private fun virtualFile(file: File): VirtualFile =
        checkNotNull(StandardFileSystems.local().findFileByPath(file.path)) { "no virtual file for $file" }

    private fun createProject(): Project {
        val configuration = KotlinTestUtils.newConfiguration(
            ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, emptyList(), listOf(javaFilesDir)
        )

        @OptIn(CoreEnvironmentDeprecation::class)
        val environment =  KotlinCoreEnvironment.createForParallelTests(
            testRootDisposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        return environment.project
    }

    private fun configureManager(@Language("JAVA") text: String, className: String): KotlinCliJavaFileManagerImpl {
        val fooPackageDir = File(javaFilesDir, "foo")
        fooPackageDir.mkdir()

        File(fooPackageDir, "$className.java").writeText(text)

        return configureManager(JavaRoot(virtualFile(javaFilesDir), JavaRoot.RootType.SOURCE))
    }

    private fun configureManager(vararg roots: JavaRoot): KotlinCliJavaFileManagerImpl {
        // Initialize classpath/index in the manager
        VirtualFileFinder.getInstance(project, module = null)

        val coreJavaFileManager = project.getService(CoreJavaFileManager::class.java) as KotlinCliJavaFileManagerImpl

        coreJavaFileManager.initialize(
            JvmDependenciesIndexImpl(roots.toList()),
            emptyList(),
            SingleJavaFileRootsIndex(emptyList()),
            usePsiClassFilesReading = false,
            javaModuleFinder = object : JavaModuleFinder {
                // Should not matter for those tests
                override fun findModule(name: String): JavaModule? = null
            },
            perfManager = null, // Don't care about performance measurements in these custom tests
        )

        return coreJavaFileManager
    }

    private fun assertCanFind(manager: KotlinCliJavaFileManagerImpl, packageFQName: String, classFqName: String) {
        val allScope = GlobalSearchScope.allScope(project)

        val classId = ClassId(FqName(packageFQName), FqName(classFqName), isLocal = false)
        val stringRequest = classId.asSingleFqName().asString()

        val foundByClassId = (manager.findClass(classId, allScope) as JavaClassImpl).psi
        val foundByString = manager.findClass(stringRequest, allScope)

        assertNotNull(foundByClassId) { "Could not find: $classId" }
        assertNotNull(foundByString) { "Could not find: $stringRequest" }

        assertEquals(foundByClassId, foundByString)
        assertEquals(
            "$packageFQName.$classFqName",
            foundByClassId.qualifiedName
        ) {
            "Found ${foundByClassId.qualifiedName} instead of $packageFQName"
        }
    }

    private fun assertCannotFind(manager: KotlinCliJavaFileManagerImpl, packageFQName: String, classFqName: String) {
        val classId = ClassId(FqName(packageFQName), FqName(classFqName), isLocal = false)
        val foundClass = manager.findClass(classId, GlobalSearchScope.allScope(project))
        assertNull(foundClass) { "Found, but shouldn't have: $classId" }
    }

    private companion object {
        private val LIB_ID = ClassId(FqName("p"), Name.identifier("Lib"))
        private val REF_ID = ClassId(FqName("p"), Name.identifier("Ref"))
    }
}
