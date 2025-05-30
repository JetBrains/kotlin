/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils.inferencelogs

import kotlin.math.abs

/**
 * [hue] - in degrees, [saturation] - [0, 1], [lightness] - [0, 1].
 */
private fun hslToRgb(hue: Double, saturation: Double, lightness: Double): Triple<Int, Int, Int> {
    val c = (1 - abs(2 * lightness - 1)) * saturation
    val x = c * (1 - abs((hue / 60.0) % 2 - 1))
    val m = lightness - c / 2

    val nonNormalized = when {
        0 <= hue && hue < 60 -> Triple(c, x, 0.0)
        60 <= hue && hue < 120 -> Triple(x, c, 0.0)
        120 <= hue && hue < 180 -> Triple(0.0, c, x)
        180 <= hue && hue < 240 -> Triple(0.0, x, c)
        240 <= hue && hue < 300 -> Triple(x, 0.0, c)
        300 <= hue && hue < 360 -> Triple(c, 0.0, x)
        else -> error("Incorrect hue value in: ($hue, $saturation, $lightness)")
    }

    return ((nonNormalized + Triple(m, m, m)) * 255.0).toInts()
}

private operator fun Triple<Double, Double, Double>.plus(other: Triple<Double, Double, Double>) =
    Triple(first + other.first, second + other.second, third + other.third)

private operator fun Triple<Double, Double, Double>.times(scalar: Double) = map { it * scalar }

private fun Triple<Double, Double, Double>.toInts() = map { it.toInt() }

private fun <T, K> Triple<T, T, T>.map(transform: (T) -> K) =
    Triple(transform(first), transform(second), transform(third))

private fun rgbToHex(r: Int, g: Int, b: Int): String = ((r shl 16) + (g shl 8) + b).toHexString().substring(2)

/**
 * Generates the sequence: 1/2, 1/4, 3/4, 1/8, 3/8, 5/8, 7/8, ...
 * It's like the BFS traversal of a graph where the next nodes are midpoints
 * between the previous ones.
 */
private fun traverseBinaryRationalsFrom0To1(): Sequence<Double> {
    var numerator = 1
    var denominator = 2

    return generateSequence {
        if (numerator >= denominator) {
            numerator = 1
            denominator *= 2
        }

        val result = numerator / denominator.toDouble()
        numerator += 2
        result
    }
}

/**
 * Generates a sequence of angles corresponding to points on a circle so that
 * a new point is inserted in the center between the consecutive points that
 * happen to have the greatest distance between each other.
 */
private fun generatePointsOnACircle(): Sequence<Double> {
    val circleQuarterTraversal = sequenceOf(0.0) + traverseBinaryRationalsFrom0To1()

    return circleQuarterTraversal.flatMap { offset ->
        listOf(
            offset + 0.0, // North
            offset + kotlin.math.PI, // South
            offset + kotlin.math.PI / 2, // East
            offset + kotlin.math.PI * 3.0 / 2 // West
        )
    }
}

internal fun generateHexColors(saturation: Double = 1.0, lightness: Double = 0.5): Sequence<String> {
    return generatePointsOnACircle().map {
        hslToRgb(it / kotlin.math.PI * 180, saturation, lightness).let { (r, g, b) -> rgbToHex(r, g, b) }
    }
}
