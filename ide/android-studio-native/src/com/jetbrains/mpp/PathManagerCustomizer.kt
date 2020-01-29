/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.OCPathManagerCustomization
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class PathManagerCustomization : OCPathManagerCustomization() {
    override fun getBinFile(relativePath: String): File {
        /*
         Expected layout:
         <folder-with-installed-idea-plugins>
            |- Kotlin                  (Kotlin plugin installation folder)
            |- mobile-mpp              (Mobile plugin installation folder)
            |  |- lib
            |  |  |- mobile-mpp.jar    (jar with all native-plugin classes)
            |  |  |- ... other jars needed for native-plugin ...
            |  |- native
            |     |- Bridge.framework
            |     |- JBDevice.framework
            |     |- ... other binaries needed for native-plugin ...
            |- ... other plugins installation folders ...

         */
        val mobileMppPluginJar = PathUtil.getResourcePathForClass(this::class.java).takeIf { it.name == "mobile-mpp.jar" }!!
        val libFolder = mobileMppPluginJar.parentFile
        val mobileMppPluginInstallationFolder = libFolder.parentFile
        val nativeFolder = File(mobileMppPluginInstallationFolder, "bin")

        return File(nativeFolder, relativePath).also {
            FileUtil.setExecutable(it) // FIXME omit when CLion build script is corrected
        }
    }
}