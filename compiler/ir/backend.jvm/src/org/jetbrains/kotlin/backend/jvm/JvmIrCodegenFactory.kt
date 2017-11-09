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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.PackageContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

object JvmIrCodegenFactory : CodegenFactory {
    override fun createPackageCodegen(state: GenerationState, files: Collection<KtFile>, fqName: FqName, registry: PackagePartRegistry): PackageCodegen {
        val impl = PackageCodegenImpl(state, files, fqName, registry)

        return object : PackageCodegen {
            override fun generate(errorHandler: CompilationErrorHandler) {
                JvmBackendFacade.doGenerateFiles(files, state, errorHandler)
            }

            override fun generateClassOrObject(classOrObject: KtClassOrObject, packagePartContext: PackageContext) {
                TODO()
            }

            override fun getPackageFragment(): PackageFragmentDescriptor {
                return impl.packageFragment
            }
        }
    }

    override fun createMultifileClassCodegen(state: GenerationState, files: Collection<KtFile>, fqName: FqName, registry: PackagePartRegistry): MultifileClassCodegen {
        TODO()
    }
}