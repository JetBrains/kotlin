/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.name.ClassId

interface IrGenerationExtension : IrDeserializer.IrLinkerExtension {
    companion object :
        ProjectExtensionDescriptor<IrGenerationExtension>(
            "org.jetbrains.kotlin.irGenerationExtension", IrGenerationExtension::class.java
        )

    fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)

    fun getPlatformIntrinsicExtension(backendContext: BackendContext): IrIntrinsicExtension? = null

    /**
     * A child of [IrGenerationExtension] can optionally generate an [IrClassSymbol] to use it inside the extension by overriding
     * this function.
     *
     * For example, imagine a compiler plugin that must add a `@com.example.Foo` annotation to specific classes even through the
     * source code of the classes does not have `com.example.Foo` annotation for the classes. To prepare `@com.example.Foo`, we
     * can write a child of [IrGenerationExtension] and override this function.
     *
     * The IR codegen API will call this function and provide [classIdToIrClass] that generates [IrClassSymbol] for a given [ClassId],
     * similar to the IR codegen for dependencies. [prepareIrClassUsedInsideExtension] in the child of [IrGenerationExtension] can
     * just call [classIdToIrClass] with `com.example.Foo`. The child extension can keep the generated [IrClassSymbol] and add it
     * as an annotation to the classes in [generate].
     */
    fun prepareIrClassUsedInsideExtension(classIdToIrClass: (ClassId) -> IrClassSymbol?) {}

    // Returns true if this extension should also be applied in the KAPT stub generation mode in Kotlin/JVM. This mode uses light analysis
    // in the compiler frontend to produce an "API-only" class file which is then converted to a .java stub. Because of the light analysis,
    // the resulting IR does not have function bodies and can contain references to error types. If this method returns true, the extension
    // should be ready to handle such incomplete IR. Any modifications to the IR applied during the KAPT stub generation mode will only have
    // effect on the .java stub, not the resulting .class files. Compilation to the .class files is done in a separate step after stub
    // generation.
    //
    // K2 KAPT doesn't use stub generation, so this property has no effect on extensions applied when K2 is enabled.
    @FirIncompatiblePluginAPI
    val shouldAlsoBeAppliedInKaptStubGenerationMode: Boolean get() = false
}

/**
 * This interface for common IR is empty because intrinsics are done in a platform-specific way (because of inliner).
 * Currently, only JVM intrinsics are supported via JvmIrIntrinsicExtension interface.
 */
interface IrIntrinsicExtension
