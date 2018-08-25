package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.PlatformConfigurator
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.storage.StorageManager

object KonanPlatform : TargetPlatform("Konan") {

    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {
        result.add(ImportPath.fromString("kotlin.native.*"))
    }

    override val multiTargetPlatform = MultiTargetPlatform.Specific(platformName)
    override val platformConfigurator: PlatformConfigurator = KonanPlatformConfigurator
}
