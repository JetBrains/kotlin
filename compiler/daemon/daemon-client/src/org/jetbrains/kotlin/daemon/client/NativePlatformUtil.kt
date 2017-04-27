/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.daemon.common.DaemonReportCategory
import java.io.IOException

private class NativePlatformLauncherWrapper {
    fun launch(processBuilder: ProcessBuilder): Process =
            try {
                val nativeLauncher = net.rubygrapefruit.platform.Native.get(net.rubygrapefruit.platform.ProcessLauncher::class.java)
                nativeLauncher.start(processBuilder)
            }
            catch (e: net.rubygrapefruit.platform.NativeException) {
                throw IOException(e)
            }
}


fun launchProcessWithFallback(processBuilder: ProcessBuilder, reportingTargets: DaemonReportingTargets, reportingSource: String = "process launcher"): Process =
        try {
            NativePlatformLauncherWrapper().launch(processBuilder)
        }
        catch (e: IOException) {
            reportingTargets.report(DaemonReportCategory.DEBUG, "Could not start process with native process launcher, falling back to ProcessBuilder#start (${e.cause})")
            null
        }
        catch (e: NoClassDefFoundError) {
            reportingTargets.report(DaemonReportCategory.DEBUG, "net.rubygrapefruit.platform library is not in the classpath, falling back to ProcessBuilder#start")
            null
        }
        ?: processBuilder.start()
