package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration

enum class KonanTarget(var enabled: Boolean = false) {
    IPHONE(),
    IPHONE_SIM(),
    LINUX(),
    MACBOOK()
}

class TargetManager(val config: CompilerConfiguration) {
    val targets = KonanTarget.values().associate{ it.name.toLowerCase() to it }
    val current = determineCurrent()

    init {
        when (host) {
            KonanTarget.LINUX   -> KonanTarget.LINUX.enabled = true
            KonanTarget.MACBOOK -> {
                KonanTarget.MACBOOK.enabled = true
                KonanTarget.IPHONE.enabled = true
                KonanTarget.IPHONE_SIM.enabled = true
            }
        }

        if (!current.enabled) {
            error("Target $current is not available on the current host")
        }
    }

    fun known(name: String): String {
        if (targets[name] == null) {
            error("Unknown target: $name. Use -list_targets to see the list of available targets")
        }
        return name!!
    }

    fun list() {
        targets.forEach { key, target -> 
            if (target.enabled) {
                val isDefault = if (target == current) "(default)" else ""
                println(String.format("%1$-30s%2$-10s", "$key:", "$isDefault"))
            }
        }
    }

    fun determineCurrent(): KonanTarget {
        val userRequest = config.get(KonanConfigKeys.TARGET)
        return if (userRequest == null || userRequest == "host") {
            host
        } else {
            targets[known(userRequest)]!!
        }
    }

    fun currentSuffix(): String {
        if (host == KonanTarget.MACBOOK) {
            when (current) {
                KonanTarget.MACBOOK -> return("osx")
                KonanTarget.IPHONE -> return("osx-ios")
                KonanTarget.IPHONE_SIM -> return("osx-ios-sim")
                else -> error("Impossible combination of $host and $current")
            }
        }
        if (host == KonanTarget.LINUX) {
            when (current) {
                KonanTarget.LINUX -> return("linux")
                KonanTarget.IPHONE -> return("linux-ios")
                KonanTarget.IPHONE_SIM -> return("linux-ios-sim")
                else -> error("Impossible combination of $host and $current")
            }
        }
        error("Unknown host target $host)")
    }

    companion object {
        fun host_os(): String {
            val javaOsName = System.getProperty("os.name")
            return when (javaOsName) {
                "Mac OS X" -> "osx"
                "Linux"    -> "linux"
                else -> error("Unknown operating system: ${javaOsName}") 
            }
        }

        fun host_arch(): String { 
            val javaArch = System.getProperty("os.arch")
            return when (javaArch) {
                "x86_64" -> "x86_64"
                "amd64"  -> "x86_64"
                "arm64"  -> "arm64"
                else -> error("Unknown hardware platform: ${javaArch}")
            }
        }

        val host: KonanTarget = when (host_os()) {
            "osx"   -> KonanTarget.MACBOOK
            "linux" -> KonanTarget.LINUX
            else -> error("Unknown host target: ${host_os()} ${host_arch()}")
        }
    }

    val crossCompile = (host != current)
}

