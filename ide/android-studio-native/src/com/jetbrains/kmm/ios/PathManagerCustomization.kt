/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.OCPathManager
import com.jetbrains.cidr.OCPathManagerCustomization
import com.jetbrains.kmm.KMM_LOG
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class PathManagerCustomization : OCPathManagerCustomization() {
    override fun getBinFile(relativePath: String): File =
        File(getBinariesDir(), relativePath).also {
            FileUtil.setExecutable(it)
        }

    companion object {
        //KMM-365: after updating KMM plugin AS can erase execution permission for embedded binary files
        //fixme fast workaround. need more intellectual execution permission update
        fun fixExecutionPermissionForBridgeService() {
            val bridgeServiceExec = File(getBinariesDir(), "Bridge.framework/Versions/A/Resources/BridgeService")
            if (bridgeServiceExec.exists()) {
                if (!bridgeServiceExec.canExecute()) {
                    //set execution permission
                    KMM_LOG.debug("Set execution permission for BridgeService file: ${bridgeServiceExec.path}")
                    FileUtil.setExecutable(bridgeServiceExec)

                    //invalidate system service
                    val bridgeServicePlist = OCPathManager.getUserLibrarySubFile("LaunchAgents/com.jetbrains.AppCode.BridgeService.plist")
                    if (bridgeServicePlist.exists()) {
                        KMM_LOG.debug("Unload BridgeService")
                        GeneralCommandLine("launchctl", "unload", bridgeServicePlist.absolutePath).createProcess().waitFor()
                        KMM_LOG.debug("Delete BridgeService Plist")
                        bridgeServicePlist.delete()
                    } else {
                        KMM_LOG.debug("BridgeService Plist file doesn't exist yet.")
                    }
                } else {
                    KMM_LOG.debug("BridgeService file has right permissions.")
                }
            } else {
                KMM_LOG.error("BridgeService not found: ${bridgeServiceExec.path}")
            }
        }

        private fun getBinariesDir(): File {
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
            return File(kmmPluginInstallationFolder, "bin")
        }
    }
}