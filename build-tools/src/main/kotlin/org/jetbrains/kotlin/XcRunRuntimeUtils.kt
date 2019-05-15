package org.jetbrains.kotlin

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.Xcode
import kotlin.math.min

/**
 * Compares two strings assuming that both are representing numeric version strings.
 * Examples of numeric version strings: "12.4.1.2", "9", "0.5".
 */
private fun compareStringsAsVersions(version1: String, version2: String): Int {
    val version1 = version1.split('.').map { it.toInt() }
    val version2 = version2.split('.').map { it.toInt() }
    val minimalLength = min(version1.size, version2.size)
    for (index in 0 until minimalLength) {
        if (version1[index] < version2[index]) return -1
        if (version1[index] > version2[index]) return 1
    }
    return version1.size.compareTo(version2.size)
}

/**
 * Returns parsed output of `xcrun simctl list runtimes -j`.
 */
private fun Xcode.getSimulatorRuntimeDescriptors(): List<SimulatorRuntimeDescriptor> =
     Json.nonstrict.parse(ListRuntimesReport.serializer(), this.simulatorRuntimes).runtimes

/**
 * Returns first available simulator runtime for [target] with at least [osMinVersion] OS version.
 * */
fun Xcode.getLatestSimulatorRuntimeFor(target: KonanTarget, osMinVersion: String): SimulatorRuntimeDescriptor? {
    val osName = when (target) {
        KonanTarget.IOS_X64 -> "iOS"
        KonanTarget.WATCHOS_X64 -> "watchOS"
        KonanTarget.TVOS_X64 -> "tvOS"
        else -> error("Unexpected simulator target: $target")
    }
    return getSimulatorRuntimeDescriptors().firstOrNull {
        it.isAvailable && it.name.startsWith(osName) && compareStringsAsVersions(it.version, osMinVersion) >= 0
    }
}

// Result of `xcrun simctl list runtimes -j`.
@Serializable
data class ListRuntimesReport(
        val runtimes: List<SimulatorRuntimeDescriptor>
)

@Serializable
data class SimulatorRuntimeDescriptor(
        val version: String,
        val bundlePath: String,
        val isAvailable: Boolean,
        val name: String,
        val identifier: String,
        val buildversion: String
)
