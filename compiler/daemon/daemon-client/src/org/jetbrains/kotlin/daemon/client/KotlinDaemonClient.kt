/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import java.io.File

typealias DaemonId = Int

sealed class KotlinDaemonClient {
    abstract fun compile(options: CompilationOptions, compilerArguments: List<String>/*, progressListener: ProgressListener?*/): ExitCode

    abstract fun getAvailableDaemons(): List<DaemonId>

    abstract fun getDaemonInfo(daemonId: DaemonId)

    abstract fun stopDaemon(daemonId: DaemonId)
}

sealed interface KotlinDaemonClientProtocol<T : ProtocolSettings> {
    object Rmi : KotlinDaemonClientProtocol<ProtocolSettings.RmiSettings>
    object Http : KotlinDaemonClientProtocol<ProtocolSettings.HttpSettings>
}

class DaemonSettings<T : ProtocolSettings>(
    val compilerClasspath: List<File>,
    val jvmArgs: List<String>,
    val protocol: KotlinDaemonClientProtocol<T>,
    val protocolSettings: T
)

sealed interface ProtocolSettings {
    class RmiSettings() : ProtocolSettings
    class HttpSettings(/*host: String, port: Int*/) : ProtocolSettings
}

// TODO: think more about this interface
internal class RmiKotlinDaemonClient(val daemonSettings: DaemonSettings<ProtocolSettings.RmiSettings>) : KotlinDaemonClient() {
    override fun compile(options: CompilationOptions, compilerArguments: List<String>): ExitCode {
        return ExitCode.OK
    }

    override fun getAvailableDaemons(): List<DaemonId> {
        TODO("Not yet implemented")
    }

    override fun getDaemonInfo(daemonId: DaemonId) {
        TODO("Not yet implemented")
    }

    override fun stopDaemon(daemonId: DaemonId) {
        TODO("Not yet implemented")
    }
}

internal class HttpKotlinDaemonClient(val daemonSettings: DaemonSettings<ProtocolSettings.HttpSettings>) : KotlinDaemonClient() {
    override fun compile(options: CompilationOptions, compilerArguments: List<String>): ExitCode {
        TODO("Not yet implemented")
    }

    override fun getAvailableDaemons(): List<DaemonId> {
        TODO("Not yet implemented")
    }

    override fun getDaemonInfo(daemonId: DaemonId) {
        TODO("Not yet implemented")
    }

    override fun stopDaemon(daemonId: DaemonId) {
        TODO("Not yet implemented")
    }
}

fun <T : ProtocolSettings> getKotlinDaemonClient(settings: DaemonSettings<T>) = when (settings.protocol) {
    KotlinDaemonClientProtocol.Rmi -> RmiKotlinDaemonClient(settings as DaemonSettings<ProtocolSettings.RmiSettings>)
    KotlinDaemonClientProtocol.Http -> HttpKotlinDaemonClient(settings as DaemonSettings<ProtocolSettings.HttpSettings>)
}