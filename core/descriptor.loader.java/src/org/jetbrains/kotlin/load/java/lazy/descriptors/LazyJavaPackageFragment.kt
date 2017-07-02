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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.resolveAnnotations
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.getValue

class LazyJavaPackageFragment(
        private val c: LazyJavaResolverContext,
        private val jPackage: JavaPackage
) : PackageFragmentDescriptorImpl(c.module, jPackage.fqName) {
    internal val binaryClasses by c.storageManager.createLazyValue {
        c.components.packageMapper.findPackageParts(fqName.asString()).mapNotNull { partName ->
            val classId = ClassId(fqName, Name.identifier(partName))
            c.components.kotlinClassFinder.findKotlinClass(classId)?.let { partName to it }
        }.toMap()
    }

    private val scope = JvmPackageScope(c, jPackage, this)

    private val subPackages = c.storageManager.createRecursionTolerantLazyValue(
            { jPackage.subPackages.map(JavaPackage::fqName) },
            // This breaks infinite recursion between loading Java descriptors and building light classes
            onRecursiveCall = listOf()
    )

    private val packageAnnotations = c.resolveAnnotations(jPackage)

    // Test only method
    fun getPackageAnnotations() = packageAnnotations

    internal fun getSubPackageFqNames(): List<FqName> = subPackages()

    internal fun findClassifierByJavaClass(jClass: JavaClass): ClassDescriptor? = scope.javaScope.findClassifierByJavaClass(jClass)

    private val partToFacade by c.storageManager.createLazyValue {
        val result = hashMapOf<String, String>()
        kotlinClasses@for ((partName, kotlinClass) in binaryClasses) {
            val header = kotlinClass.classHeader
            when (header.kind) {
                KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                    val facadeName = header.multifileClassName ?: continue@kotlinClasses
                    result[partName] = facadeName.substringAfterLast('/')
                }
                KotlinClassHeader.Kind.FILE_FACADE -> {
                    result[partName] = partName
                }
                else -> {}
            }
        }
        result
    }

    fun getFacadeSimpleNameForPartSimpleName(partName: String): String? = partToFacade[partName]

    override fun getMemberScope() = scope

    override fun toString() = "Lazy Java package fragment: $fqName"

    override fun getSource(): SourceElement = KotlinJvmBinaryPackageSourceElement(this)
}
