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

package org.jetbrains.kotlin.resolve.lazy

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor

interface TopLevelDescriptorProvider {
    fun getPackageFragment(fqName: FqName): LazyPackageDescriptor?

    fun getTopLevelClassifierDescriptors(fqName: FqName, location: LookupLocation): Collection<ClassifierDescriptor>

    fun assertValid()
}

object NoTopLevelDescriptorProvider : TopLevelDescriptorProvider {
    private fun shouldNotBeCalled(): Nothing = throw UnsupportedOperationException("Should not be called")

    override fun getPackageFragment(fqName: FqName): LazyPackageDescriptor? {
        shouldNotBeCalled()
    }

    override fun getTopLevelClassifierDescriptors(fqName: FqName, location: LookupLocation): Collection<ClassifierDescriptor> {
        shouldNotBeCalled()
    }

    override fun assertValid() {
        shouldNotBeCalled()
    }
}
