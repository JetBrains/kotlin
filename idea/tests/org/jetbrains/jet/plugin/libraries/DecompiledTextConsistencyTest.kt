/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.libraries

import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.jet.lang.resolve.name.FqName
import org.junit.Assert
import org.jetbrains.jet.plugin.PluginTestCaseBase
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils
import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import java.util.Collections
import org.jetbrains.jet.plugin.caches.resolve.JavaResolveExtension
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

public class DecompiledTextConsistencyTest : JetLightCodeInsightFixtureTestCase() {

    private val STANDARD_LIBRARY_FQNAME = FqName("kotlin")

    public fun testConsistencyWithJavaDescriptorResolver() {
        val packageClassFqName = PackageClassUtils.getPackageClassFqName(STANDARD_LIBRARY_FQNAME)
        val kotlinPackageClass = myFixture.getJavaFacade()!!.findClass(packageClassFqName.asString())!!
        val kotlinPackageFile = kotlinPackageClass.getContainingFile()!!.getVirtualFile()!!
        val projectBasedText = buildDecompiledText(kotlinPackageFile, ProjectBasedResolverForDecompiler(getProject())).text
        val deserializerForDecompiler = DeserializerForDecompiler(kotlinPackageFile.getParent()!!, STANDARD_LIBRARY_FQNAME)
        val deserializedText = buildDecompiledText(kotlinPackageFile, deserializerForDecompiler).text
        Assert.assertEquals(projectBasedText, deserializedText)
        // sanity checks
        Assert.assertTrue(projectBasedText.contains("linkedListOf"))
        Assert.assertFalse(projectBasedText.contains("ERROR"))
    }

    override fun getProjectDescriptor() = object : JetWithJdkAndRuntimeLightProjectDescriptor() {
        override fun getSdk() = PluginTestCaseBase.fullJdk()
    }
}

class ProjectBasedResolverForDecompiler(project: Project) : ResolverForDecompiler {
    val javaDescriptorResolver = JavaResolveExtension[project]

    override fun resolveClass(classFqName: FqName): ClassDescriptor? {
        return javaDescriptorResolver.resolveClass(classFqName)
    }

    override fun resolveDeclarationsInPackage(packageFqName: FqName): Collection<DeclarationDescriptor> {
        val packageView = javaDescriptorResolver.getModule().getPackage(packageFqName)
        if (packageView == null) {
            return Collections.emptyList()
        }
        return packageView.getMemberScope().getAllDescriptors() filter {
            it is CallableDescriptor && DescriptorUtils.getContainingModule(it) != KotlinBuiltIns.getInstance().getBuiltInsModule()
        }
    }
}