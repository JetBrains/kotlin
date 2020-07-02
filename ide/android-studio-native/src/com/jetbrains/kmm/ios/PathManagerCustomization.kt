/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

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
            |- kmm                     (Mobile plugin installation folder)
            |  |- lib
            |  |  |- kmm.jar    (jar with all native-plugin classes)
            |  |  |- ... other jars needed for native-plugin ...
            |  |- bin
            |     |- Bridge.framework
            |     |- JBDevice.framework
            |     |- ... other binaries needed for native-plugin ...
            |- ... other plugins installation folders ...

         */
        val kmmPluginJar = PathUtil.getResourcePathForClass(this::class.java).takeIf { it.name == "kmm.jar" }!!
        val libFolder = kmmPluginJar.parentFile
        val kmmPluginInstallationFolder = libFolder.parentFile
        val nativeFolder = File(kmmPluginInstallationFolder, "bin")

        return File(nativeFolder, relativePath).also {
            FileUtil.setExecutable(it) // FIXME omit when CLion build script is corrected
        }
    }
}