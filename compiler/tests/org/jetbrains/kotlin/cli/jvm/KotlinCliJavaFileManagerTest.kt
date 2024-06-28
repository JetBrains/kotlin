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
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.search.GlobalSearchScope
import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class KotlinCliJavaFileManagerTest : KotlinTestWithEnvironment() {
    private lateinit var javaFilesDir: File

    fun testCommon() {
        val manager = configureManager(
                "package foo;\n" +
                "\n" +
                "public class TopLevel {\n" +
                "public class Inner {\n" +
                "   public class Inner {}\n" +
                "}\n" +
                "\n" +
                "}",
                "TopLevel")

        assertCanFind(manager, "foo", "TopLevel")
        assertCanFind(manager, "foo", "TopLevel.Inner")
        assertCanFind(manager, "foo", "TopLevel.Inner.Inner")

        assertCannotFind(manager, "foo", "TopLevel\$Inner.Inner")
        assertCannotFind(manager, "foo", "TopLevel.Inner\$Inner")
        assertCannotFind(manager, "foo", "TopLevel.Inner.Inner.Inner")
    }

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

    fun testTopLevelClassWithDollarsAndInners() {
        val manager = configureManager("package foo;\n\n" + "public class Top\$Level\$\$ {\n" +
                                       "public class I\$nner {" + "   public class I\$nner{}" + "   public class In\$ne\$r\${}" + "   public class Inner\$\$\$\$\${}" + "   public class \$Inner{}" + "   public class \${}" + "   public class \$\$\$\$\${}" + "}\n" + "public class Inner {" + "   public class Inner{}" + "}\n" + "\n" + "}", "Top\$Level\$\$")

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

    fun testDoNotThrowOnMalformedInput() {
        val fileWithEmptyName = configureManager("package foo;\n\n public class Top\$Level {}", "")
        val allScope = GlobalSearchScope.allScope(project)
        fileWithEmptyName.findClass("foo.", allScope)
        fileWithEmptyName.findClass(".", allScope)
        fileWithEmptyName.findClass("..", allScope)
        fileWithEmptyName.findClass(".foo", allScope)
    }

    fun testSeveralClassesInOneFile() {
        val manager = configureManager("package foo;\n\n" + "public class One {}\n" + "class Two {}\n" + "class Three {}", "One")

        assertCanFind(manager, "foo", "One")

        //NOTE: this is unsupported
        assertCannotFind(manager, "foo", "Two")
        assertCannotFind(manager, "foo", "Three")
    }

    fun testScopeCheck() {
        val manager = configureManager("package foo;\n\n" + "public class Test {}\n", "Test")

        TestCase.assertNotNull("Should find class in all scope", manager.findClass("foo.Test", GlobalSearchScope.allScope(project)))
        TestCase.assertNull("Should not find class in empty scope", manager.findClass("foo.Test", GlobalSearchScope.EMPTY_SCOPE))
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        javaFilesDir = KtTestUtil.tmpDir("java-file-manager-test")

        val configuration = KotlinTestUtils.newConfiguration(
            ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, emptyList(), listOf(javaFilesDir)
        )

        return KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private fun configureManager(@Language("JAVA") text: String, className: String): KotlinCliJavaFileManagerImpl {
        val fooPackageDir = File(javaFilesDir, "foo")
        fooPackageDir.mkdir()

        File(fooPackageDir, "$className.java").writeText(text)

        @Suppress("UNUSED_VARIABLE") // used to implicitly initialize classpath/index in the manager
        val coreJavaFileFinder = VirtualFileFinder.SERVICE.getInstance(project)
        val coreJavaFileManager = project.getService(CoreJavaFileManager::class.java) as KotlinCliJavaFileManagerImpl

        val root = StandardFileSystems.local().findFileByPath(javaFilesDir.path)!!
        coreJavaFileManager.initialize(
            JvmDependenciesIndexImpl(listOf(JavaRoot(root, JavaRoot.RootType.SOURCE))),
            emptyList(),
            SingleJavaFileRootsIndex(emptyList()),
            usePsiClassFilesReading = false
        )

        return coreJavaFileManager
    }

    private fun assertCanFind(manager: KotlinCliJavaFileManagerImpl, packageFQName: String, classFqName: String) {
        val allScope = GlobalSearchScope.allScope(project)

        val classId = ClassId(FqName(packageFQName), FqName(classFqName), isLocal = false)
        val stringRequest = classId.asSingleFqName().asString()

        val foundByClassId = (manager.findClass(classId, allScope) as JavaClassImpl).psi
        val foundByString = manager.findClass(stringRequest, allScope)

        TestCase.assertNotNull("Could not find: $classId", foundByClassId)
        TestCase.assertNotNull("Could not find: $stringRequest", foundByString)

        TestCase.assertEquals(foundByClassId, foundByString)
        TestCase.assertEquals("Found ${foundByClassId.qualifiedName} instead of $packageFQName", packageFQName + "." + classFqName,
                              foundByClassId.qualifiedName)
    }

    private fun assertCannotFind(manager: KotlinCliJavaFileManagerImpl, packageFQName: String, classFqName: String) {
        val classId = ClassId(FqName(packageFQName), FqName(classFqName), isLocal = false)
        val foundClass = manager.findClass(classId, GlobalSearchScope.allScope(project))
        TestCase.assertNull("Found, but shouldn't have: $classId", foundClass)
    }
}
