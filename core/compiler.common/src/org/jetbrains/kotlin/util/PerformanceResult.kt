/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util
enum class PlatformType {
    JVM,
    JS,
    Common,
    Native,
}

interface TimeStats {
    val time: Time
}

@Serializable
class InitStats(
    val filesCount: Int,
    val linesCount: Int,
    override val time: Time,
) : TimeStats

@Serializable
class FirStats(
    val allNodesCount: Int,
    val leafNodesCount: Int,
    val starImportsCount: Int,
    override val time: Time,
) : TimeStats

@Serializable
class IrStats(
    val allNodesCountAfter: Int,
    val leafNodesCountAfter: Int,
    override val time: Time,
) : TimeStats

@Serializable
class BinaryStats(
    val count: Int,
    val bytesCount: Long,
    override val time: Time,
) : TimeStats

@Serializable
data class PerformanceMeasurementResult(
    val moduleName: String,
    val platform: PlatformType = PlatformType.JVM,
    val isK2: Boolean = true,
    val hasErrors: Boolean = false,
    val initStats: InitStats? = null,
    val firStats: FirStats? = null,
    val irStats: IrStats? = null,
    val irLoweringStats: IrStats? = null,
    val backendStats: BinaryStats? = null,
    val findJavaClassStats: BinaryStats? = null,
    val findBinaryClassStats: BinaryStats? = null,
    val gcStats: GarbageCollectionMeasurement,
    val jitTimeMilliseconds: Long,
)