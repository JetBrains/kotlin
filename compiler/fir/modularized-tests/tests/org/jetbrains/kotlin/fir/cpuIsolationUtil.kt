/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.sun.jna.Library
import com.sun.jna.Native
import java.io.File

private interface CLibrary : Library {
    fun getpid(): Int
    fun gettid(): Int

    companion object {
        val INSTANCE = Native.load("c", CLibrary::class.java) as CLibrary
    }
}


/**
 * Pins the current thread to an isolated CPU.
 *
 * This method attempts to pin the current thread to an isolated CPU if the environment variables
 * 'DOCKER_ISOLATED_CPUSET' and 'DOCKER_CPUSET' are set.
 *
 * On benchmark agents, those variables should be assigned to non-overlapping CPUSETs.
 * It allows us to reduce interference of the other processes and threads.
 *
 * Note: CPUSET is in a format compatible with 'taskset' command (e.g., "0-3,8")
 */
internal fun pinCurrentThreadToIsolatedCpu() {
    val isolatedList = System.getenv("DOCKER_ISOLATED_CPUSET")
    val othersList = System.getenv("DOCKER_CPUSET")
    println("Trying to set affinity, other: '$othersList', isolated: '$isolatedList'")
    if (othersList != null) {
        // Move all processes (including self) to the DOCKER_CPUSET
        updateAffinityOfAllProcesses(othersList)
    }
    if (isolatedList != null) {
        // Move the current thread to the isolated cpuset
        // Must be called after the updateAffinityOfAllProcesses, otherwise it wouldn't have an effect
        updateCurrentThreadAffinity(isolatedList)
    }
    if (othersList == null && isolatedList == null) {
        println("No affinity specified")
    }
}


/**
 * Updates the CPU affinity of the current thread.
 */
private fun updateCurrentThreadAffinity(cpuList: String) {
    val selfPid = CLibrary.INSTANCE.getpid()
    val selfTid = CLibrary.INSTANCE.gettid()
    println("Will pin self affinity, my pid: $selfPid, my tid: $selfTid")
    ProcessBuilder().command("taskset", "-cp", cpuList, "$selfTid").inheritIO().start().waitFor()
}


/**
 * Updates the affinity of all processes in the system to the specified CPU list.
 *
 * This method iterates over all processes in the system and modifies their affinity
 * to the CPUs specified in the 'cpuList' parameter.
 */
private fun updateAffinityOfAllProcesses(cpuList: String) {
    println("Will move others affinity to '$cpuList'")
    val pidRegex = "[0-9]+".toRegex()
    File("/proc/").listFiles()?.forEach {
        if (it.resolve("stat").exists() && it.name.matches(pidRegex)) {
            ProcessBuilder().command("taskset", "-cap", cpuList, it.name).inheritIO().start().waitFor()
        }
    }
}