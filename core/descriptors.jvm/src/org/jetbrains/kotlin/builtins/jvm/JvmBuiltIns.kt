/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.jvm

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.sure

class JvmBuiltIns(storageManager: StorageManager, kind: Kind) : KotlinBuiltIns(storageManager) {
    /**
     * Where built-ins should be loaded from.
     */
    enum class Kind {
        /**
         * Load built-ins from dependencies of the module that's being compiled. In this case, any request to a built-in class such as
         * `kotlin.String` would end up querying contents of the module and all its dependencies in order until the first one returns
         * anything, similarly to how it's done for normal (.class file-based) dependencies. If there's a class with FQ name `kotlin.String`
         * in module sources, it'll be returned in [KotlinBuiltIns.getString], otherwise it'll be loaded from the first dependency that
         * has the `kotlin/kotlin.kotlin_builtins` file where metadata of `kotlin.String` is located.
         *
         * If this mode is selected, the module should be injected after an instance of [JvmBuiltIns] is created via setting
         * [KotlinBuiltIns.builtInsModule] to point to the module that's being compiled.
         *
         * This mode is preferred and should be used in new code when possible.
         */
        FROM_DEPENDENCIES,

        /**
         * Load built-ins by looking up `.kotlin_builtins` resources in the class loader of the current compiler or IDE plugin.
         *
         * This mode is discouraged and should be avoided when possible. The reason is that in case versions of the compiler and the
         * standard library in compilation dependencies do not match, there can be tricky errors or even differences in behavior if
         * built-ins API has changed.
         */
        FROM_CLASS_LOADER,

        /**
         * Similarly to [FROM_CLASS_LOADER], load built-ins from the compiler class loader, but also mark the loaded package
         * fragments as "fallback" (`BuiltInsPackageFragment.isFallback`) to make the compiler report errors on any usages of elements
         * from these built-ins.
         *
         * This mode is useful as a "secondary" built-ins source to ensure there are at least some built-ins for the compiler frontend to
         * work correctly. Such fallback built-ins are usually placed at the end of the dependencies list, so that built-ins from the
         * standard library (which are present in >99% cases) would win in the resolution.
         */
        FALLBACK,
    }

    // Module containing JDK classes or having them among dependencies
    private var ownerModuleDescriptor: ModuleDescriptor? = null
    private var isAdditionalBuiltInsFeatureSupported: Boolean = true

    fun initialize(moduleDescriptor: ModuleDescriptor, isAdditionalBuiltInsFeatureSupported: Boolean) {
        assert(ownerModuleDescriptor == null) { "JvmBuiltins repeated initialization" }
        this.ownerModuleDescriptor = moduleDescriptor
        this.isAdditionalBuiltInsFeatureSupported = isAdditionalBuiltInsFeatureSupported
    }

    val settings: JvmBuiltInsSettings by storageManager.createLazyValue {
        JvmBuiltInsSettings(
            builtInsModule, storageManager,
            { ownerModuleDescriptor.sure { "JvmBuiltins has not been initialized properly" } },
            {
                ownerModuleDescriptor.sure { "JvmBuiltins has not been initialized properly" }
                isAdditionalBuiltInsFeatureSupported
            }
        )
    }

    init {
        when (kind) {
            Kind.FROM_DEPENDENCIES -> {
            }
            Kind.FROM_CLASS_LOADER -> createBuiltInsModule(false)
            Kind.FALLBACK -> createBuiltInsModule(true)
        }
    }

    override fun getPlatformDependentDeclarationFilter(): PlatformDependentDeclarationFilter = settings

    override fun getAdditionalClassPartsProvider(): AdditionalClassPartsProvider = settings

    override fun getClassDescriptorFactories() =
        super.getClassDescriptorFactories() + JvmBuiltInClassDescriptorFactory(storageManager, builtInsModule)
}
