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

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.getValue

class LazyJavaPackageFragment(
        private val c: LazyJavaResolverContext,
        private val jPackage: JavaPackage
) : PackageFragmentDescriptorImpl(c.module, jPackage.getFqName()) {
    private val scope by c.storageManager.createLazyValue {
        LazyJavaPackageScope(c, jPackage, this)
    }

    internal val kotlinBinaryClasses by c.storageManager.createLazyValue {
        c.components.packageMapper.findPackageParts(fqName.asString()).mapNotNull {
            val classId = ClassId(fqName, Name.identifier(it))
            c.components.kotlinClassFinder.findKotlinClass(classId)
        }
    }

    override fun getMemberScope() = scope

    override fun toString() = "lazy java package fragment: $fqName"

    override fun getSource(): SourceElement {
        return KotlinJvmBinaryPackageSourceElement(jPackage, kotlinBinaryClasses)
    }
}
