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

package  org.jetbrains.kotlin.native.interop.tool

import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.properties.*

// TODO: Half of these calculations are already provided 
// by ClangHost class of "shared" project.
// But there are some discrepancies, in particular the -B claculation.
fun KonanProperties.defaultCompilerOpts(): List<String> {

    // TODO: eliminate this. below
    val targetToolchain = absoluteTargetToolchain
    val targetSysRoot = absoluteTargetSysRoot
    val llvmHome = absoluteLlvmHome
    val llvmVersion = hostString("llvmVersion")!!

    // StubGenerator passes the arguments to libclang which
    // works not exactly the same way as the clang binary and
    // (in particular) uses different default header search path.
    // See e.g. http://lists.llvm.org/pipermail/cfe-dev/2013-November/033680.html
    // We workaround the problem with -isystem flag below.
    val isystem = "$llvmHome/lib/clang/$llvmVersion/include"
    val quadruple = targetString("quadruple")
    val arch = targetString("arch")
    val archSelector = if (quadruple != null)
        listOf("-target", quadruple) else listOf("-arch", arch!!)
    val commonArgs = listOf("-isystem", isystem, "--sysroot=$targetSysRoot")

    val host = TargetManager.host
    val hostSpecificArgs = when (host) {
        MACBOOK -> {
            val osVersionMinFlag = targetString("osVersionMinFlagClang")
            val osVersionMinValue = targetString("osVersionMin")
            listOf("-B$targetToolchain/bin") +
                    (if (osVersionMinFlag != null && osVersionMinValue != null)
                        listOf("$osVersionMinFlag=$osVersionMinValue") else emptyList())
        }
        LINUX -> {
            val libGcc = targetString("libGcc")
            val binDir = "$targetSysRoot/${libGcc ?: "bin"}"
            listOf(
                    "-B$binDir", "--gcc-toolchain=$targetToolchain",
                    "-fuse-ld=$targetToolchain/bin/ld")
        }
        MINGW -> {
            listOf("-B$targetSysRoot/bin")
        }
        else -> error("Unexpected host: ${host}")
    }

    return archSelector + commonArgs + hostSpecificArgs
}

