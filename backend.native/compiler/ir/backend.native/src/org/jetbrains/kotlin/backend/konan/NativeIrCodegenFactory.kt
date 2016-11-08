package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.PackageContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject


object NativeIrCodegenFactory: CodegenFactory {
    override fun createPackageCodegen(state: GenerationState, files: Collection<org.jetbrains.kotlin.psi.KtFile>, fqName: org.jetbrains.kotlin.name.FqName, registry: PackagePartRegistry): PackageCodegen {
        val impl = PackageCodegenImpl(state, files, fqName, registry)


        return object : PackageCodegen {
            override fun generate(errorHandler: CompilationErrorHandler) {
                NativeBackendFacade.doGenerateFiles(files, state, errorHandler)
            }

            override fun generateClassOrObject(classOrObject: KtClassOrObject, packagePartContext: PackageContext) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getPackageFragment(): PackageFragmentDescriptor {
                return impl.packageFragment
            }
        }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createMultifileClassCodegen(state: GenerationState, files: Collection<org.jetbrains.kotlin.psi.KtFile>, fqName: org.jetbrains.kotlin.name.FqName, registry: PackagePartRegistry): MultifileClassCodegen {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

