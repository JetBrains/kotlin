/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

// Note: This file is just a concatenation of stdlib_external/numbers/MathTest.kt and
// stdlib_external/numbers/HarmonyMathTests.kt with `assertFails` calls removed.
// The `assertFails` method checks exception throwing so it causes chash on wasm32.

// TODO: We need to remove this file and use the originals from stdlib_external
// as soon as excptions become supported for wasm.

package test.numbers

import kotlin.math.*
import kotlin.test.*

fun assertAlmostEquals(expected: Double, actual: Double, tolerance: Double? = null) {
    val tolerance_ = tolerance?.let { abs(it) } ?: 0.000000000001
    if (abs(expected - actual) > tolerance_) {
        assertEquals(expected, actual)
    }
}

fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Double? = null) {
    val tolerance_ = tolerance?.let { abs(it) } ?: 0.0000001
    if (abs(expected - actual) > tolerance_) {
        assertEquals(expected, actual)
    }
}

class DoubleMathTest {

    @Test fun trigonometric() {
        assertEquals(0.0, sin(0.0))
        assertAlmostEquals(0.0, sin(PI))

        assertEquals(0.0, asin(0.0))
        assertAlmostEquals(PI / 2, asin(1.0))

        assertEquals(1.0, cos(0.0))
        assertAlmostEquals(-1.0, cos(PI))

        assertEquals(0.0, acos(1.0))
        assertAlmostEquals(PI / 2, acos(0.0))

        assertEquals(0.0, tan(0.0))
        assertAlmostEquals(1.0, tan(PI / 4))

        assertAlmostEquals(0.0, atan(0.0))
        assertAlmostEquals(PI / 4, atan(1.0))

        assertAlmostEquals(PI / 4, atan2(10.0, 10.0))
        assertAlmostEquals(-PI / 4, atan2(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
        assertAlmostEquals(0.0, atan2(0.0, 0.0))
        assertAlmostEquals(0.0, atan2(0.0, 10.0))
        assertAlmostEquals(PI / 2, atan2(2.0, 0.0))

        for (angle in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertTrue(sin(angle).isNaN(), "sin($angle)")
            assertTrue(cos(angle).isNaN(), "cos($angle)")
            assertTrue(tan(angle).isNaN(), "tan($angle)")
        }

        for (value in listOf(Double.NaN, 1.2, -1.1)) {
            assertTrue(asin(value).isNaN())
            assertTrue(acos(value).isNaN())
        }
        assertTrue(atan(Double.NaN).isNaN())
        assertTrue(atan2(Double.NaN, 0.0).isNaN())
        assertTrue(atan2(0.0, Double.NaN).isNaN())
    }

    @Test fun hyperbolic() {
        assertEquals(Double.POSITIVE_INFINITY, sinh(Double.POSITIVE_INFINITY))
        assertEquals(Double.NEGATIVE_INFINITY, sinh(Double.NEGATIVE_INFINITY))
        assertTrue(sinh(Double.MIN_VALUE) != 0.0)
        assertTrue(sinh(710.0).isFinite())
        assertTrue(sinh(-710.0).isFinite())
        assertTrue(sinh(Double.NaN).isNaN())

        assertEquals(Double.POSITIVE_INFINITY, cosh(Double.POSITIVE_INFINITY))
        assertEquals(Double.POSITIVE_INFINITY, cosh(Double.NEGATIVE_INFINITY))
        assertTrue(cosh(710.0).isFinite())
        assertTrue(cosh(-710.0).isFinite())
        assertTrue(cosh(Double.NaN).isNaN())

        assertAlmostEquals(1.0, tanh(Double.POSITIVE_INFINITY))
        assertAlmostEquals(-1.0, tanh(Double.NEGATIVE_INFINITY))
        assertTrue(tanh(Double.MIN_VALUE) != 0.0)
        assertTrue(tanh(Double.NaN).isNaN())
    }

    @Test fun inverseHyperbolicSin() {
        for (exact in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, Double.MIN_VALUE, -Double.MIN_VALUE, 0.00001)) {
            assertEquals(exact, asinh(sinh(exact)))
        }
        for (approx in listOf(Double.MIN_VALUE, 0.1, 1.0, 100.0, 710.0)) {
            assertAlmostEquals(approx, asinh(sinh(approx)))
            assertAlmostEquals(-approx, asinh(sinh(-approx)))
        }
        assertTrue(asinh(Double.NaN).isNaN())
    }

    @Test fun inverseHyperbolicCos() {
        for (exact in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0)) {
            assertEquals(abs(exact), acosh(cosh(exact)))
        }
        for (approx in listOf(Double.MIN_VALUE, 0.00001, 1.0, 100.0, 710.0)) {
            assertAlmostEquals(approx, acosh(cosh(approx)))
            assertAlmostEquals(approx, acosh(cosh(-approx)))
        }
        for (invalid in listOf(-1.0, 0.0, 0.99999, Double.NaN)) {
            assertTrue(acosh(invalid).isNaN())
        }
    }

    @Test fun inverseHyperbolicTan() {
        for (exact in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, Double.MIN_VALUE, -Double.MIN_VALUE)) {
            assertEquals(exact, atanh(tanh(exact)))
        }
        for (approx in listOf(0.00001)) {
            assertAlmostEquals(approx, atanh(tanh(approx)))
        }

        for (invalid in listOf(-1.00001, 1.00001, Double.NaN, Double.MAX_VALUE, -Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertTrue(atanh(invalid).isNaN())
        }
    }

    @Test fun powers() {
        assertEquals(5.0, hypot(3.0, 4.0))
        assertEquals(Double.POSITIVE_INFINITY, hypot(Double.NEGATIVE_INFINITY, Double.NaN))
        assertEquals(Double.POSITIVE_INFINITY, hypot(Double.NaN, Double.POSITIVE_INFINITY))
        assertTrue(hypot(Double.NaN, 0.0).isNaN())

        assertEquals(1.0, Double.NaN.pow(0.0))
        assertEquals(1.0, Double.POSITIVE_INFINITY.pow(0))
        assertEquals(49.0, 7.0.pow(2))
        assertEquals(0.25, 2.0.pow(-2))
        assertTrue(0.0.pow(Double.NaN).isNaN())
        assertTrue(Double.NaN.pow(-1).isNaN())
        assertTrue((-7.0).pow(1/3.0).isNaN())
        assertTrue(1.0.pow(Double.POSITIVE_INFINITY).isNaN())
        assertTrue((-1.0).pow(Double.NEGATIVE_INFINITY).isNaN())

        assertEquals(5.0, sqrt(9.0 + 16.0))
        assertTrue(sqrt(-1.0).isNaN())
        assertTrue(sqrt(Double.NaN).isNaN())

        assertTrue(exp(Double.NaN).isNaN())
        assertAlmostEquals(E, exp(1.0))
        assertEquals(1.0, exp(0.0))
        assertEquals(0.0, exp(Double.NEGATIVE_INFINITY))
        assertEquals(Double.POSITIVE_INFINITY, exp(Double.POSITIVE_INFINITY))

        assertEquals(0.0, expm1(0.0))
        assertEquals(Double.MIN_VALUE, expm1(Double.MIN_VALUE))
        assertEquals(0.00010000500016667084, expm1(1e-4))
        assertEquals(-1.0, expm1(Double.NEGATIVE_INFINITY))
        assertEquals(Double.POSITIVE_INFINITY, expm1(Double.POSITIVE_INFINITY))
    }

    @Test fun logarithms() {
        assertTrue(log(1.0, Double.NaN).isNaN())
        assertTrue(log(Double.NaN, 1.0).isNaN())
        assertTrue(log(-1.0, 2.0).isNaN())
        assertTrue(log(2.0, -1.0).isNaN())
        assertTrue(log(2.0, 0.0).isNaN())
        assertTrue(log(2.0, 1.0).isNaN())
        assertTrue(log(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY).isNaN())
        assertEquals(-2.0, log(0.25, 2.0))
        assertEquals(-0.5, log(2.0, 0.25))
        assertEquals(Double.NEGATIVE_INFINITY, log(Double.POSITIVE_INFINITY, 0.25))
        assertEquals(Double.POSITIVE_INFINITY, log(Double.POSITIVE_INFINITY, 2.0))
        assertEquals(Double.NEGATIVE_INFINITY, log(0.0, 2.0))
        assertEquals(Double.POSITIVE_INFINITY, log(0.0, 0.25))

        assertTrue(ln(Double.NaN).isNaN())
        assertTrue(ln(-1.0).isNaN())
        assertEquals(1.0, ln(E))
        assertEquals(Double.NEGATIVE_INFINITY, ln(0.0))
        assertEquals(Double.POSITIVE_INFINITY, ln(Double.POSITIVE_INFINITY))

        assertEquals(1.0, log10(10.0))
        assertAlmostEquals(-1.0, log10(0.1))

        assertAlmostEquals(3.0, log2(8.0))
        assertEquals(-1.0, log2(0.5))

        assertTrue(ln1p(Double.NaN).isNaN())
        assertTrue(ln1p(-1.1).isNaN())
        assertEquals(0.0, ln1p(0.0))
        assertEquals(9.999995000003334e-7, ln1p(1e-6))
        assertEquals(Double.MIN_VALUE, ln1p(Double.MIN_VALUE))
        assertEquals(Double.NEGATIVE_INFINITY, ln1p(-1.0))
    }

    @Test fun rounding() {
        for (value in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, 1.0, -10.0)) {
            assertEquals(value, ceil(value))
            assertEquals(value, floor(value))
            assertEquals(value, truncate(value))
            assertEquals(value, round(value))
        }
        assertTrue(ceil(Double.NaN).isNaN())
        assertTrue(floor(Double.NaN).isNaN())
        assertTrue(truncate(Double.NaN).isNaN())
        assertTrue(round(Double.NaN).isNaN())
        val data = arrayOf( //   v floor trunc round  ceil
                doubleArrayOf( 1.3,  1.0,  1.0,  1.0,  2.0),
                doubleArrayOf(-1.3, -2.0, -1.0, -1.0, -1.0),
                doubleArrayOf( 1.5,  1.0,  1.0,  2.0,  2.0),
                doubleArrayOf(-1.5, -2.0, -1.0, -2.0, -1.0),
                doubleArrayOf( 1.8,  1.0,  1.0,  2.0,  2.0),
                doubleArrayOf(-1.8, -2.0, -1.0, -2.0, -1.0),

                doubleArrayOf( 2.3,  2.0,  2.0,  2.0,  3.0),
                doubleArrayOf(-2.3, -3.0, -2.0, -2.0, -2.0),
                doubleArrayOf( 2.5,  2.0,  2.0,  2.0,  3.0),
                doubleArrayOf(-2.5, -3.0, -2.0, -2.0, -2.0),
                doubleArrayOf( 2.8,  2.0,  2.0,  3.0,  3.0),
                doubleArrayOf(-2.8, -3.0, -2.0, -3.0, -2.0)
        )
        for ((v, f, t, r, c) in data) {
            assertEquals(f, floor(v), "floor($v)")
            assertEquals(t, truncate(v), "truncate($v)")
            assertEquals(r, round(v), "round($v)")
            assertEquals(c, ceil(v), "ceil($v)")
        }
    }

    @Test fun roundingConversion() {
        assertEquals(1L, 1.0.roundToLong())
        assertEquals(1L, 1.1.roundToLong())
        assertEquals(2L, 1.5.roundToLong())
        assertEquals(3L, 2.5.roundToLong())
        assertEquals(-2L, (-2.5).roundToLong())
        assertEquals(-3L, (-2.6).roundToLong())
        assertEquals(9223372036854774784, (9223372036854774800.0).roundToLong())
        assertEquals(Long.MAX_VALUE, Double.MAX_VALUE.roundToLong())
        assertEquals(Long.MIN_VALUE, (-Double.MAX_VALUE).roundToLong())
        assertEquals(Long.MAX_VALUE, Double.POSITIVE_INFINITY.roundToLong())
        assertEquals(Long.MIN_VALUE, Double.NEGATIVE_INFINITY.roundToLong())

        assertEquals(1, 1.0.roundToInt())
        assertEquals(1, 1.1.roundToInt())
        assertEquals(2, 1.5.roundToInt())
        assertEquals(3, 2.5.roundToInt())
        assertEquals(-2, (-2.5).roundToInt())
        assertEquals(-3, (-2.6).roundToInt())
        assertEquals(2123456789, (2123456789.0).roundToInt())
        assertEquals(Int.MAX_VALUE, Double.MAX_VALUE.roundToInt())
        assertEquals(Int.MIN_VALUE, (-Double.MAX_VALUE).roundToInt())
        assertEquals(Int.MAX_VALUE, Double.POSITIVE_INFINITY.roundToInt())
        assertEquals(Int.MIN_VALUE, Double.NEGATIVE_INFINITY.roundToInt())
    }

    @Test fun absoluteValue() {
        assertTrue(abs(Double.NaN).isNaN())
        assertTrue(Double.NaN.absoluteValue.isNaN())

        for (value in listOf(0.0, Double.MIN_VALUE, 0.1, 1.0, 1000.0, Double.MAX_VALUE, Double.POSITIVE_INFINITY)) {
            assertEquals(value, value.absoluteValue)
            assertEquals(value, (-value).absoluteValue)
            assertEquals(value, abs(value))
            assertEquals(value, abs(-value))
        }
    }

    @Test fun signs() {
        assertTrue(sign(Double.NaN).isNaN())
        assertTrue(Double.NaN.sign.isNaN())

        val negatives = listOf(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -1.0, -Double.MIN_VALUE)
        for (value in negatives) {
            assertEquals(-1.0, sign(value))
            assertEquals(-1.0, value.sign)
        }

        val zeroes = listOf(0.0, -0.0)
        for (value in zeroes) {
            assertEquals(value, sign(value))
            assertEquals(value, value.sign)
        }


        val positives = listOf(Double.POSITIVE_INFINITY, Double.MAX_VALUE, 1.0, Double.MIN_VALUE)
        for (value in positives) {
            assertEquals(1.0, sign(value))
            assertEquals(1.0, value.sign)
        }

        val allValues = negatives + positives
        for (a in allValues) {
            for (b in allValues) {
                val r = a.withSign(b)
                assertEquals(a.absoluteValue, r.absoluteValue)
                assertEquals(b.sign, r.sign, "expected $a with sign bit of $b to have sign ${b.sign}")
            }

            val rp0 = a.withSign(0.0)
            assertEquals(1.0, rp0.sign)
            assertEquals(a.absoluteValue, rp0.absoluteValue)

            val rm0 = a.withSign(-0.0)
            assertEquals(-1.0, rm0.sign)
            assertEquals(a.absoluteValue, rm0.absoluteValue)

            val ri = a.withSign(-1)
            assertEquals(-1.0, ri.sign)
            assertEquals(a.absoluteValue, ri.absoluteValue)

            val rn = a.withSign(Double.NaN)
            assertEquals(a.absoluteValue, rn.absoluteValue)
        }
    }

    @Test fun nextAndPrev() {
        for (value in listOf(0.0, -0.0, Double.MIN_VALUE, -1.0, 2.0.pow(10))) {
            val next = value.nextUp()
            if (next > 0) {
                assertEquals(next, value + value.ulp)
            } else {
                assertEquals(value, next - next.ulp)
            }

            val prev = value.nextDown()
            if (prev > 0) {
                assertEquals(value, prev + prev.ulp)
            }
            else {
                assertEquals(prev, value - value.ulp)
            }

            val toZero = value.nextTowards(0.0)
            if (toZero != 0.0) {
                assertEquals(value, toZero + toZero.ulp.withSign(toZero))
            }

            assertEquals(Double.POSITIVE_INFINITY, Double.MAX_VALUE.nextUp())
            assertEquals(Double.MAX_VALUE, Double.POSITIVE_INFINITY.nextDown())

            assertEquals(Double.NEGATIVE_INFINITY, (-Double.MAX_VALUE).nextDown())
            assertEquals((-Double.MAX_VALUE), Double.NEGATIVE_INFINITY.nextUp())

            assertTrue(Double.NaN.ulp.isNaN())
            assertTrue(Double.NaN.nextDown().isNaN())
            assertTrue(Double.NaN.nextUp().isNaN())
            assertTrue(Double.NaN.nextTowards(0.0).isNaN())

            assertEquals(Double.MIN_VALUE, (0.0).ulp)
            assertEquals(Double.MIN_VALUE, (-0.0).ulp)
            assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY.ulp)
            assertEquals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY.ulp)

            val maxUlp = 2.0.pow(971)
            assertEquals(maxUlp, Double.MAX_VALUE.ulp)
            assertEquals(maxUlp, (-Double.MAX_VALUE).ulp)
        }
    }

    @Test fun IEEEremainder() {
        val data = arrayOf(  //  a    a IEEErem 2.5
                doubleArrayOf(-2.0,   0.5),
                doubleArrayOf(-1.25, -1.25),
                doubleArrayOf( 0.0,   0.0),
                doubleArrayOf( 1.0,   1.0),
                doubleArrayOf( 1.25,  1.25),
                doubleArrayOf( 1.5,  -1.0),
                doubleArrayOf( 2.0,  -0.5),
                doubleArrayOf( 2.5,   0.0),
                doubleArrayOf( 3.5,   1.0),
                doubleArrayOf( 3.75, -1.25),
                doubleArrayOf( 4.0,  -1.0)
        )
        for ((a, r) in data) {
            assertEquals(r, a.IEEErem(2.5), "($a).IEEErem(2.5)")
        }

        assertTrue(Double.NaN.IEEErem(2.5).isNaN())
        assertTrue(2.0.IEEErem(Double.NaN).isNaN())
        assertTrue(Double.POSITIVE_INFINITY.IEEErem(2.0).isNaN())
        assertTrue(2.0.IEEErem(0.0).isNaN())
        assertEquals(PI, PI.IEEErem(Double.NEGATIVE_INFINITY))
    }

    /*
     * Special cases:
     *   - `atan2(0.0, 0.0)` is `0.0`
     *   - `atan2(0.0, x)` is  `0.0` for `x > 0` and `PI` for `x < 0`
     *   - `atan2(-0.0, x)` is `-0.0` for 'x > 0` and `-PI` for `x < 0`
     *   - `atan2(y, +Inf)` is `0.0` for `0 < y < +Inf` and `-0.0` for '-Inf < y < 0`
     *   - `atan2(y, -Inf)` is `PI` for `0 < y < +Inf` and `-PI` for `-Inf < y < 0`
     *   - `atan2(y, 0.0)` is `PI/2` for `y > 0` and `-PI/2` for `y < 0`
     *   - `atan2(+Inf, x)` is `PI/2` for finite `x`y
     *   - `atan2(-Inf, x)` is `-PI/2` for finite `x`
     *   - `atan2(NaN, x)` and `atan2(y, NaN)` is `NaN`
     */
    @Test fun atan2SpecialCases() {

        assertEquals(atan2(0.0, 0.0), 0.0)
        assertEquals(atan2(0.0, 1.0), 0.0)
        assertEquals(atan2(0.0, -1.0), PI)
        assertEquals(atan2(-0.0, 1.0), -0.0)
        assertEquals(atan2(-0.0, -1.0), -PI)
        assertEquals(atan2(1.0, Double.POSITIVE_INFINITY), 0.0)
        assertEquals(atan2(-1.0, Double.POSITIVE_INFINITY), -0.0)
        assertEquals(atan2(1.0, Double.NEGATIVE_INFINITY), PI)
        assertEquals(atan2(-1.0, Double.NEGATIVE_INFINITY), -PI)
        assertEquals(atan2(1.0, 0.0), PI/2)
        assertEquals(atan2(-1.0, 0.0), -PI/2)
        assertEquals(atan2(Double.POSITIVE_INFINITY, 1.0), PI/2)
        assertEquals(atan2(Double.NEGATIVE_INFINITY, 1.0), -PI/2)

        assertTrue(atan2(Double.NaN, 1.0).isNaN())
        assertTrue(atan2(1.0, Double.NaN).isNaN())
    }
}

class FloatMathTest {

    companion object {
        const val PI = kotlin.math.PI.toFloat()
        const val E = kotlin.math.E.toFloat()
    }

    @Test fun trigonometric() {
        assertEquals(0.0F, sin(0.0F))
        assertAlmostEquals(0.0F, sin(PI))

        assertEquals(0.0F, asin(0.0F))
        assertAlmostEquals(PI / 2, asin(1.0F), 0.0000002)

        assertEquals(1.0F, cos(0.0F))
        assertAlmostEquals(-1.0F, cos(PI))

        assertEquals(0.0F, acos(1.0F))
        assertAlmostEquals(PI / 2, acos(0.0F))

        assertEquals(0.0F, tan(0.0F))
        assertAlmostEquals(1.0F, tan(PI / 4))

        assertAlmostEquals(0.0F, atan(0.0F))
        assertAlmostEquals(PI / 4, atan(1.0F))

        assertAlmostEquals(PI / 4, atan2(10.0F, 10.0F))
        assertAlmostEquals(-PI / 4, atan2(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY))
        assertAlmostEquals(0.0F, atan2(0.0F, 0.0F))
        assertAlmostEquals(0.0F, atan2(0.0F, 10.0F))
        assertAlmostEquals(PI / 2, atan2(2.0F, 0.0F))

        for (angle in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertTrue(sin(angle).isNaN(), "sin($angle)")
            assertTrue(cos(angle).isNaN(), "cos($angle)")
            assertTrue(tan(angle).isNaN(), "tan($angle)")
        }

        for (value in listOf(Float.NaN, 1.2F, -1.1F)) {
            assertTrue(asin(value).isNaN())
            assertTrue(acos(value).isNaN())
        }
        assertTrue(atan(Float.NaN).isNaN())
        assertTrue(atan2(Float.NaN, 0.0F).isNaN())
        assertTrue(atan2(0.0F, Float.NaN).isNaN())
    }

    @Test fun hyperbolic() {
        assertEquals(Float.POSITIVE_INFINITY, sinh(Float.POSITIVE_INFINITY))
        assertEquals(Float.NEGATIVE_INFINITY, sinh(Float.NEGATIVE_INFINITY))
        assertTrue(sinh(Float.MIN_VALUE) != 0.0F)
        assertTrue(sinh(89.0F).isFinite())
        assertTrue(sinh(-89.0F).isFinite())
        assertTrue(sinh(Float.NaN).isNaN())

        assertEquals(Float.POSITIVE_INFINITY, cosh(Float.POSITIVE_INFINITY))
        assertEquals(Float.POSITIVE_INFINITY, cosh(Float.NEGATIVE_INFINITY))
        assertTrue(cosh(89.0F).isFinite())
        assertTrue(cosh(-89.0F).isFinite())
        assertTrue(cosh(Float.NaN).isNaN())

        assertAlmostEquals(1.0F, tanh(Float.POSITIVE_INFINITY))
        assertAlmostEquals(-1.0F, tanh(Float.NEGATIVE_INFINITY))
        assertTrue(tanh(Float.MIN_VALUE) != 0.0F)
        assertTrue(tanh(Float.NaN).isNaN())
    }

    @Test fun inverseHyperbolicSin() {
        for (exact in listOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, Float.MIN_VALUE, -Float.MIN_VALUE, 0.00001F)) {
            assertEquals(exact, asinh(sinh(exact)))
        }
        for (approx in listOf(Float.MIN_VALUE, 0.1F, 1.0F, 89.0F)) {
            assertAlmostEquals(approx, asinh(sinh(approx)))
            assertAlmostEquals(-approx, asinh(sinh(-approx)))
        }
        assertTrue(asinh(Float.NaN).isNaN())
    }

    @Test fun inverseHyperbolicCos() {
        for (exact in listOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F)) {
            assertEquals(abs(exact), acosh(cosh(exact)))
        }
        for (approx in listOf(Float.MIN_VALUE, 0.1F, 1.0F, 89.0F)) {
            assertAlmostEquals(approx, acosh(cosh(approx)))
            assertAlmostEquals(approx, acosh(cosh(-approx)))
        }
        for (invalid in listOf(-1.0F, 0.0F, 0.99999F, Float.NaN)) {
            assertTrue(acosh(invalid).isNaN())
        }
    }

    @Test fun inverseHyperbolicTan() {
        for (exact in listOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, Float.MIN_VALUE, -Float.MIN_VALUE)) {
            assertEquals(exact, atanh(tanh(exact)))
        }

        for (approx in listOf(0.00001F)) {
            assertAlmostEquals(approx, atanh(tanh(approx)))
        }

        for (invalid in listOf(-1.00001F, 1.00001F, Float.NaN, Float.MAX_VALUE, -Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)) {
            assertTrue(atanh(invalid).isNaN())
        }
    }

    @Test fun powers() {
        assertEquals(5.0F, hypot(3.0F, 4.0F))
        assertEquals(Float.POSITIVE_INFINITY, hypot(Float.NEGATIVE_INFINITY, Float.NaN))
        assertEquals(Float.POSITIVE_INFINITY, hypot(Float.NaN, Float.POSITIVE_INFINITY))
        assertTrue(hypot(Float.NaN, 0.0F).isNaN())

        assertEquals(1.0F, Float.NaN.pow(0.0F))
        assertEquals(1.0F, Float.POSITIVE_INFINITY.pow(0))
        assertEquals(49.0F, 7.0F.pow(2))
        assertEquals(0.25F, 2.0F.pow(-2))
        assertTrue(0.0F.pow(Float.NaN).isNaN())
        assertTrue(Float.NaN.pow(-1).isNaN())
        assertTrue((-7.0F).pow(1/3.0F).isNaN())
        assertTrue(1.0F.pow(Float.POSITIVE_INFINITY).isNaN())
        assertTrue((-1.0F).pow(Float.NEGATIVE_INFINITY).isNaN())

        assertEquals(5.0F, sqrt(9.0F + 16.0F))
        assertTrue(sqrt(-1.0F).isNaN())
        assertTrue(sqrt(Float.NaN).isNaN())

        assertTrue(exp(Float.NaN).isNaN())
        assertAlmostEquals(E, exp(1.0F))
        assertEquals(1.0F, exp(0.0F))
        assertEquals(0.0F, exp(Float.NEGATIVE_INFINITY))
        assertEquals(Float.POSITIVE_INFINITY, exp(Float.POSITIVE_INFINITY))

        assertEquals(0.0F, expm1(0.0F))
        assertEquals(-1.0F, expm1(Float.NEGATIVE_INFINITY))
        assertEquals(Float.POSITIVE_INFINITY, expm1(Float.POSITIVE_INFINITY))
    }

    @Test fun logarithms() {
        assertTrue(log(1.0F, Float.NaN).isNaN())
        assertTrue(log(Float.NaN, 1.0F).isNaN())
        assertTrue(log(-1.0F, 2.0F).isNaN())
        assertTrue(log(2.0F, -1.0F).isNaN())
        assertTrue(log(2.0F, 0.0F).isNaN())
        assertTrue(log(2.0F, 1.0F).isNaN())
        assertTrue(log(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY).isNaN())
        assertEquals(-2.0F, log(0.25F, 2.0F))
        assertEquals(-0.5F, log(2.0F, 0.25F))
        assertEquals(Float.NEGATIVE_INFINITY, log(Float.POSITIVE_INFINITY, 0.25F))
        assertEquals(Float.POSITIVE_INFINITY, log(Float.POSITIVE_INFINITY, 2.0F))
        assertEquals(Float.NEGATIVE_INFINITY, log(0.0F, 2.0F))
        assertEquals(Float.POSITIVE_INFINITY, log(0.0F, 0.25F))

        assertTrue(ln(Float.NaN).isNaN())
        assertTrue(ln(-1.0F).isNaN())
        assertAlmostEquals(1.0F, ln(E))
        assertEquals(Float.NEGATIVE_INFINITY, ln(0.0F))
        assertEquals(Float.POSITIVE_INFINITY, ln(Float.POSITIVE_INFINITY))

        assertEquals(1.0F, log10(10.0F))
        assertAlmostEquals(-1.0F, log10(0.1F))

        assertAlmostEquals(3.0F, log2(8.0F))
        assertEquals(-1.0F, log2(0.5F))

        assertTrue(ln1p(Float.NaN).isNaN())
        assertTrue(ln1p(-1.1F).isNaN())
        assertEquals(0.0F, ln1p(0.0F))
        assertEquals(Float.NEGATIVE_INFINITY, ln1p(-1.0F))
    }

    @Test fun rounding() {
        for (value in listOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, 1.0F, -10.0F)) {
            assertEquals(value, ceil(value))
            assertEquals(value, floor(value))
            assertEquals(value, truncate(value))
            assertEquals(value, round(value))
        }
        assertTrue(ceil(Float.NaN).isNaN())
        assertTrue(floor(Float.NaN).isNaN())
        assertTrue(truncate(Float.NaN).isNaN())
        assertTrue(round(Float.NaN).isNaN())
        val data = arrayOf( //   v floor trunc round  ceil
                floatArrayOf( 1.3F,  1.0F,  1.0F,  1.0F,  2.0F),
                floatArrayOf(-1.3F, -2.0F, -1.0F, -1.0F, -1.0F),
                floatArrayOf( 1.5F,  1.0F,  1.0F,  2.0F,  2.0F),
                floatArrayOf(-1.5F, -2.0F, -1.0F, -2.0F, -1.0F),
                floatArrayOf( 1.8F,  1.0F,  1.0F,  2.0F,  2.0F),
                floatArrayOf(-1.8F, -2.0F, -1.0F, -2.0F, -1.0F),

                floatArrayOf( 2.3F,  2.0F,  2.0F,  2.0F,  3.0F),
                floatArrayOf(-2.3F, -3.0F, -2.0F, -2.0F, -2.0F),
                floatArrayOf( 2.5F,  2.0F,  2.0F,  2.0F,  3.0F),
                floatArrayOf(-2.5F, -3.0F, -2.0F, -2.0F, -2.0F),
                floatArrayOf( 2.8F,  2.0F,  2.0F,  3.0F,  3.0F),
                floatArrayOf(-2.8F, -3.0F, -2.0F, -3.0F, -2.0F)
        )
        for ((v, f, t, r, c) in data) {
            assertEquals(f, floor(v), "floor($v)")
            assertEquals(t, truncate(v), "truncate($v)")
            assertEquals(r, round(v), "round($v)")
            assertEquals(c, ceil(v), "ceil($v)")
        }
    }

    @Test fun roundingConversion() {
        assertEquals(1L, 1.0F.roundToLong())
        assertEquals(1L, 1.1F.roundToLong())
        assertEquals(2L, 1.5F.roundToLong())
        assertEquals(3L, 2.5F.roundToLong())
        assertEquals(-2L, (-2.5F).roundToLong())
        assertEquals(-3L, (-2.6F).roundToLong())
        // assertEquals(9223372036854774784, (9223372036854774800.0F).roundToLong()) // platform-specific
        assertEquals(Long.MAX_VALUE, Float.MAX_VALUE.roundToLong())
        assertEquals(Long.MIN_VALUE, (-Float.MAX_VALUE).roundToLong())
        assertEquals(Long.MAX_VALUE, Float.POSITIVE_INFINITY.roundToLong())
        assertEquals(Long.MIN_VALUE, Float.NEGATIVE_INFINITY.roundToLong())

        assertEquals(1, 1.0F.roundToInt())
        assertEquals(1, 1.1F.roundToInt())
        assertEquals(2, 1.5F.roundToInt())
        assertEquals(3, 2.5F.roundToInt())
        assertEquals(-2, (-2.5F).roundToInt())
        assertEquals(-3, (-2.6F).roundToInt())
        assertEquals(16777218, (16777218F).roundToInt())
        assertEquals(Int.MAX_VALUE, Float.MAX_VALUE.roundToInt())
        assertEquals(Int.MIN_VALUE, (-Float.MAX_VALUE).roundToInt())
        assertEquals(Int.MAX_VALUE, Float.POSITIVE_INFINITY.roundToInt())
        assertEquals(Int.MIN_VALUE, Float.NEGATIVE_INFINITY.roundToInt())
    }

    @Test fun absoluteValue() {
        assertTrue(abs(Float.NaN).isNaN())
        assertTrue(Float.NaN.absoluteValue.isNaN())

        for (value in listOf(0.0F, Float.MIN_VALUE, 0.1F, 1.0F, 1000.0F, Float.MAX_VALUE, Float.POSITIVE_INFINITY)) {
            assertEquals(value, value.absoluteValue)
            assertEquals(value, (-value).absoluteValue)
            assertEquals(value, abs(value))
            assertEquals(value, abs(-value))
        }
    }

    @Test fun signs() {
        assertTrue(sign(Float.NaN).isNaN())
        assertTrue(Float.NaN.sign.isNaN())

        val negatives = listOf(Float.NEGATIVE_INFINITY, -Float.MAX_VALUE, -1.0F, -Float.MIN_VALUE)
        for (value in negatives) {
            assertEquals(-1.0F, sign(value))
            assertEquals(-1.0F, value.sign)
        }

        val zeroes = listOf(0.0F, -0.0F)
        for (value in zeroes) {
            assertEquals(value, sign(value))
            assertEquals(value, value.sign)
        }


        val positives = listOf(Float.POSITIVE_INFINITY, Float.MAX_VALUE, 1.0F, Float.MIN_VALUE)
        for (value in positives) {
            assertEquals(1.0F, sign(value))
            assertEquals(1.0F, value.sign)
        }

        val allValues = negatives + positives
        for (a in allValues) {
            for (b in allValues) {
                val r = a.withSign(b)
                assertEquals(a.absoluteValue, r.absoluteValue)
                assertEquals(b.sign, r.sign)
            }

            val rp0 = a.withSign(0.0F)
            assertEquals(1.0F, rp0.sign)
            assertEquals(a.absoluteValue, rp0.absoluteValue)

            val rm0 = a.withSign(-0.0F)
            assertEquals(-1.0F, rm0.sign)
            assertEquals(a.absoluteValue, rm0.absoluteValue)

            val ri = a.withSign(-1)
            assertEquals(-1.0F, ri.sign)
            assertEquals(a.absoluteValue, ri.absoluteValue)
        }
    }

    @Test fun nextAndPrev() {
        for (value in listOf(0.0f, -0.0f, Float.MIN_VALUE, -1.0f, 2.0f.pow(10))) {
            val next = value.nextUp()
            if (next > 0) {
                assertEquals(next, value + value.ulp)
            } else {
                assertEquals(value, next - next.ulp)
            }

            val prev = value.nextDown()
            if (prev > 0) {
                assertEquals(value, prev + prev.ulp)
            }
            else {
                assertEquals(prev, value - value.ulp)
            }

            val toZero = value.nextTowards(0.0f)
            if (toZero != 0.0f) {
                assertEquals(value, toZero + toZero.ulp.withSign(toZero))
            }

            assertEquals(Float.POSITIVE_INFINITY, Float.MAX_VALUE.nextUp())
            assertEquals(Float.MAX_VALUE, Float.POSITIVE_INFINITY.nextDown())

            assertEquals(Float.NEGATIVE_INFINITY, (-Float.MAX_VALUE).nextDown())
            assertEquals((-Float.MAX_VALUE), Float.NEGATIVE_INFINITY.nextUp())

            assertTrue(Float.NaN.ulp.isNaN())
            assertTrue(Float.NaN.nextDown().isNaN())
            assertTrue(Float.NaN.nextUp().isNaN())
            assertTrue(Float.NaN.nextTowards(0.0f).isNaN())

            assertEquals(Float.MIN_VALUE, (0.0f).ulp)
            assertEquals(Float.MIN_VALUE, (-0.0f).ulp)
            assertEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY.ulp)
            assertEquals(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY.ulp)

            val maxUlp = 2.0f.pow(104)
            assertEquals(maxUlp, Float.MAX_VALUE.ulp)
            assertEquals(maxUlp, (-Float.MAX_VALUE).ulp)
        }
    }

    @Test fun IEEEremainder() {
        val data = arrayOf(  //  a    a IEEErem 2.5
                floatArrayOf(-2.0f,   0.5f),
                floatArrayOf(-1.25f, -1.25f),
                floatArrayOf( 0.0f,   0.0f),
                floatArrayOf( 1.0f,   1.0f),
                floatArrayOf( 1.25f,  1.25f),
                floatArrayOf( 1.5f,  -1.0f),
                floatArrayOf( 2.0f,  -0.5f),
                floatArrayOf( 2.5f,   0.0f),
                floatArrayOf( 3.5f,   1.0f),
                floatArrayOf( 3.75f, -1.25f),
                floatArrayOf( 4.0f,  -1.0f)
        )
        for ((a, r) in data) {
            assertEquals(r, a.IEEErem(2.5f), "($a).IEEErem(2.5f)")
        }

        assertTrue(Float.NaN.IEEErem(2.5f).isNaN())
        assertTrue(2.0f.IEEErem(Float.NaN).isNaN())
        assertTrue(Float.POSITIVE_INFINITY.IEEErem(2.0f).isNaN())
        assertTrue(2.0f.IEEErem(0.0f).isNaN())
        assertEquals(PI.toFloat(), PI.toFloat().IEEErem(Float.NEGATIVE_INFINITY))
    }

}

class IntegerMathTest {

    @Test
    fun intSigns() {
        val negatives = listOf(Int.MIN_VALUE, -65536, -1)
        val positives = listOf(1, 100, 256, Int.MAX_VALUE)
        negatives.forEach { assertEquals(-1, it.sign) }
        positives.forEach { assertEquals(1, it.sign) }
        assertEquals(0, 0.sign)

        (negatives - Int.MIN_VALUE).forEach { assertEquals(-it, it.absoluteValue) }
        assertEquals(Int.MIN_VALUE, Int.MIN_VALUE.absoluteValue)

        positives.forEach { assertEquals(it, it.absoluteValue) }
    }


    @Test
    fun longSigns() {
        val negatives = listOf(Long.MIN_VALUE, -65536L, -1L)
        val positives = listOf(1L, 100L, 256L, Long.MAX_VALUE)
        negatives.forEach { assertEquals(-1, it.sign) }
        positives.forEach { assertEquals(1, it.sign) }
        assertEquals(0, 0L.sign)

        (negatives - Long.MIN_VALUE).forEach { assertEquals(-it, it.absoluteValue) }
        assertEquals(Long.MIN_VALUE, Long.MIN_VALUE.absoluteValue)

        positives.forEach { assertEquals(it, it.absoluteValue) }
    }
}


class HarmonyMath {

    fun assertEquals(expected: Double, actual: Double, tolerance: Double? = null) =
            assertEquals(null, expected, actual, tolerance)

    fun assertEquals(expected: Float, actual: Float, tolerance: Float? = null) =
            assertEquals(null, expected, actual, tolerance)

    fun Double.Companion.isNaN(v: Double) = v.isNaN()
    fun Float.Companion.isNaN(v: Float) = v.isNaN()

    fun pow(a: Double, b: Double) = a.pow(b)
    fun pow(a: Float, b: Float) = a.pow(b)

    fun ulp(v: Double) = v.ulp
    fun ulp(v: Float) = v.ulp

    fun assertEquals(message: String?, expected: Int, actual: Int) = assertEquals(expected, actual, message)
    fun assertEquals(message: String?, expected: Long, actual: Long) = assertEquals(expected, actual, message)

    fun assertEquals(message: String?, expected: Double, actual: Double, tolerance: Double? = null) {
        val tolerance_ = tolerance?.let { abs(it) } ?: 0.000000000001
        if (abs(expected - actual) > tolerance_) {
            assertEquals(expected, actual, message)
        }
    }

    fun assertEquals(message: String?, expected: Float, actual: Float, tolerance: Float? = null) {
        val tolerance_ = tolerance?.let { abs(it) } ?: 0.0000001f
        if (abs(expected - actual) > tolerance_) {
            assertEquals(expected, actual, message)
        }
    }

    fun assertTrue(message: String? = null, condition: Boolean) = assertTrue(condition, message)

    internal var HYP = sqrt(2.0)

    internal var OPP = 1.0

    internal var ADJ = 1.0

    /* Required to make previous preprocessor flags work - do not remove */
    internal var unused = 0

    /**
     * Tests kotlin.math.abs(Double)
     */
    @Test fun absD() {
        // Test for abs(Double): Double

        assertTrue("Incorrect Double abs value",
                abs(-1908.8976) == 1908.8976)
        assertTrue("Incorrect Double abs value",
                abs(1908.8976) == 1908.8976)
    }

    /**
     * Tests kotlin.math.abs(float)
     */
    @Test fun absF() {
        // Test for abs(float): float
        assertTrue("Incorrect float abs value",
                abs(-1908.8976f) == 1908.8976f)
        assertTrue("Incorrect float abs value",
                abs(1908.8976f) == 1908.8976f)
    }

    /**
     * Tests kotlin.math.abs(int)
     */
    @Test fun absI() {
        // Test for abs(int): int
        assertTrue("Incorrect int abs value", abs(-1908897) == 1908897)
        assertTrue("Incorrect int abs value", abs(1908897) == 1908897)
    }

    /**
     * Tests kotlin.math.abs(long)
     */
    @Test fun absJ() {
        // Test for abs(long): long
        assertTrue("Incorrect long abs value",
                abs(-19088976000089L) == 19088976000089L)
        assertTrue("Incorrect long abs value",
                abs(19088976000089L) == 19088976000089L)
    }

    /**
     * Tests kotlin.math.acos(Double)
     */
    @Test fun acosD() {
        // Test for acos(Double): Double
        val r = cos(acos(ADJ / HYP))
        val lr = r.toBits()
        val t = (ADJ / HYP).toBits()
        assertTrue("Returned incorrect arc cosine", lr == t || lr + 1 == t || lr - 1 == t)
    }

    /**
     * Tests kotlin.math.asin(Double)
     */
    @Test fun asinD() {
        // Test for asin(Double): Double
        val r = sin(asin(OPP / HYP))
        val lr = r.toBits()
        val t = (OPP / HYP).toBits()
        assertTrue("Returned incorrect arc sine", lr == t || lr + 1 == t || lr - 1 == t)
    }

    /**
     * Tests kotlin.math.atan(Double)
     */
    @Test fun atanD() {
        // Test for atan(Double): Double
        val answer = tan(atan(1.0))
        assertTrue("Returned incorrect arc tangent: " + answer, answer <= 1.0 && answer >= 9.9999999999999983E-1)
    }

    /**
     * Tests kotlin.math.atan2(Double, Double)
     */
    @Test fun atan2DD() {
        // Test for atan2(Double, Double): Double
        val answer = atan(tan(1.0))
        assertTrue("Returned incorrect arc tangent: " + answer, answer <= 1.0 && answer >= 9.9999999999999983E-1)
    }

    /**
     * Tests kotlin.math.ceil(Double)
     */
    @Test fun ceilD() {
        // Test for ceil(Double): Double
        assertEquals("Incorrect ceiling for Double",
                79.0, ceil(78.89), 0.0)
        assertEquals("Incorrect ceiling for Double",
                -78.0, ceil(-78.89), 0.0)
    }

    /**
     * Tests kotlin.math.withSign(Double)
     */
    @Test fun withSign_D() {
        for (i in COPYSIGN_DD_CASES.indices) {
            val magnitude = COPYSIGN_DD_CASES[i]
            val absMagnitudeBits = abs(magnitude).toBits()
            val negMagnitudeBits = (-abs(magnitude)).toBits()

            assertTrue("The result should be NaN.", Double.isNaN(Double.NaN.withSign(magnitude)))

            for (j in COPYSIGN_DD_CASES.indices) {
                val sign = COPYSIGN_DD_CASES[j]
                val resultBits = magnitude.withSign(sign).toBits()

                if (sign > 0 || (+0.0).toBits() == sign.toBits() || 0.0.toBits() == sign.toBits()) {
                    assertEquals(
                            "If the sign is positive, the result should be positive.",
                            absMagnitudeBits, resultBits)
                }
                if (sign < 0 || (-0.0).toBits() == sign.toBits()) {
                    assertEquals(
                            "If the sign is negative, the result should be negative.",
                            negMagnitudeBits, resultBits)
                }
            }
        }

        assertTrue("The result should be NaN.", Double.isNaN(Double.NaN.withSign(Double.NaN)))
    }

    /**
     * Tests kotlin.math.withSign(Float)
     */
    @Test fun withSign_F() {
        for (i in COPYSIGN_FF_CASES.indices) {
            val magnitude = COPYSIGN_FF_CASES[i]
            val absMagnitudeBits = abs(magnitude).toBits()
            val negMagnitudeBits = (-abs(magnitude)).toBits()

            assertTrue("The result should be NaN.", Float.isNaN(Float.NaN.withSign(magnitude)))

            for (j in COPYSIGN_FF_CASES.indices) {
                val sign = COPYSIGN_FF_CASES[j]
                val resultBits = magnitude.withSign(sign).toBits()
                if (sign > 0 || (+0.0f).toBits() == sign.toBits() || 0.0f.toBits() == sign.toBits()) {
                    assertEquals(
                            "If the sign is positive, the result should be positive.",
                            absMagnitudeBits, resultBits)
                }
                if (sign < 0 || (-0.0f).toBits() == sign.toBits()) {
                    assertEquals(
                            "If the sign is negative, the result should be negative.",
                            negMagnitudeBits, resultBits)
                }
            }
        }

        assertTrue("The result should be NaN.", Float.isNaN(Float.NaN.withSign(Float.NaN)))
    }

    /**
     * Tests kotlin.math.cos(Double)
     */
    @Test fun cosD() {
        // Test for cos(Double): Double
        assertEquals("Incorrect answer", 1.0, cos(0.0), 0.0)
        assertEquals("Incorrect answer", 0.5403023058681398, cos(1.0), 0.0)
    }

    /**
     * Tests kotlin.math.cosh(Double)
     */
    @Test fun cosh_D() {
        // Test for special situations
        assertTrue(Double.isNaN(cosh(Double.NaN)))
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, cosh(Double.POSITIVE_INFINITY), 0.0)
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, cosh(Double.NEGATIVE_INFINITY), 0.0)
        assertEquals("Should return 1.0", 1.0, cosh(+0.0), 0.0)
        assertEquals("Should return 1.0", 1.0, cosh(-0.0), 0.0)

        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, cosh(1234.56), 0.0)
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, cosh(-1234.56), 0.0)
        assertEquals("Should return 1.0000000000005", 1.0000000000005, cosh(0.000001), 0.0)
        assertEquals("Should return 1.0000000000005", 1.0000000000005, cosh(-0.000001), 0.0)
        assertEquals("Should return 5.212214351945598", 5.212214351945598, cosh(2.33482), 0.0)

        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, cosh(Double.MAX_VALUE), 0.0)
        assertEquals("Should return 1.0", 1.0, cosh(Double.MIN_VALUE), 0.0)
    }

    /**
     * Tests kotlin.math.exp(Double)
     */
    @Test fun expD() {
        // Test for exp(Double): Double
        assertTrue("Incorrect answer returned for simple power", abs(exp(4.0) - E * E * E * E) < 0.1)
        assertTrue("Incorrect answer returned for larger power", ln(abs(exp(5.5)) - 5.5) < 10.0)
    }

    /**
     * Tests kotlin.math.expm1(Double)
     */
    @Test fun expm1_D() {
        // Test for special cases
        assertTrue("Should return NaN", Double.isNaN(expm1(Double.NaN)))
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, expm1(Double.POSITIVE_INFINITY), 0.0)
        assertEquals("Should return -1.0", -1.0, expm1(Double.NEGATIVE_INFINITY), 0.0)

        assertEquals(0.0.toBits(), expm1(0.0).toBits())
        assertEquals(+0.0.toBits(), expm1(+0.0).toBits())
        assertEquals((-0.0).toBits(), expm1(-0.0).toBits())

        assertEquals("Should return -9.999950000166666E-6",
                -9.999950000166666E-6, expm1(-0.00001), 0.0)
        assertEquals("Should return 1.0145103074469635E60",
                1.0145103074469635E60, expm1(138.16951162), 0.0)
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, expm1(123456789123456789123456789.4521584223), 0.0)
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, expm1(Double.MAX_VALUE), 0.0)
        assertEquals("Should return MIN_VALUE", Double.MIN_VALUE, expm1(Double.MIN_VALUE), 0.0)
    }

    /**
     * Tests kotlin.math.floor(Double)
     */
    @Test fun floorD() {
        assertEquals("Incorrect floor for int", 42.0, floor(42.0), 0.0)
        assertEquals("Incorrect floor for -int", -2.0, floor(-2.0), 0.0)
        assertEquals("Incorrect floor for zero", 0.0, floor(0.0), 0.0)

        assertEquals("Incorrect floor for +Double", 78.0, floor(78.89), 0.0)
        assertEquals("Incorrect floor for -Double", -79.0, floor(-78.89), 0.0)
        assertEquals("floor large +Double", 3.7314645675925406E19, floor(3.7314645675925406E19), 0.0)
        assertEquals("floor large -Double", -8.173521839218E12, floor(-8.173521839218E12), 0.0)
        assertEquals("floor small Double", 0.0, floor(1.11895241315E-102), 0.0)

        // Compare toString representations here since -0.0 = +0.0, and
        // NaN != NaN and we need to distinguish

        assertEquals(Double.NaN.toString(), floor(Double.NaN).toString(), "Floor failed for NaN")
        assertEquals((+0.0).toString(), floor(+0.0).toString(), "Floor failed for +0.0")
        assertEquals((-0.0).toString(), floor(-0.0).toString(), "Floor failed for -0.0")
        assertEquals(Double.POSITIVE_INFINITY.toString(), floor(Double.POSITIVE_INFINITY).toString(),
                "Floor failed for +infinity")
        assertEquals(Double.NEGATIVE_INFINITY.toString(), floor(Double.NEGATIVE_INFINITY).toString(),
                "Floor failed for -infinity")
    }

    /**
     * Tests kotlin.math.hypot(Double, Double)
     */
    @Test fun hypot_DD() {
        // Test for special cases
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, hypot(Double.POSITIVE_INFINITY,
                1.0), 0.0)
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, hypot(Double.NEGATIVE_INFINITY,
                123.324), 0.0)
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, hypot(-758.2587,
                Double.POSITIVE_INFINITY), 0.0)
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, hypot(5687.21,
                Double.NEGATIVE_INFINITY), 0.0)
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, hypot(Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY), 0.0)
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, hypot(Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY), 0.0)
        assertTrue("Should be NaN", Double.isNaN(hypot(Double.NaN,
                2342301.89843)))
        assertTrue("Should be NaN", Double.isNaN(hypot(-345.2680,
                Double.NaN)))

        assertEquals("Should return 2396424.905416697", 2396424.905416697, hypot(12322.12, -2396393.2258), 0.0)
        assertEquals("Should return 138.16958070558556", 138.16958070558556,
                hypot(-138.16951162, 0.13817035864), 0.0)
        assertEquals("Should return 1.7976931348623157E308",
                1.7976931348623157E308, hypot(Double.MAX_VALUE, 211370.35), 0.0)
        assertEquals("Should return 5413.7185", 5413.7185, hypot(
                -5413.7185, Double.MIN_VALUE), 0.0)
    }

    /**
     * Tests kotlin.math.IEEEremainder(Double, Double)
     */
    @Test fun IEEEremainderDD() {
        // Test for IEEEremainder(Double, Double): Double
        assertEquals("Incorrect remainder returned",
                0.0, 1.0.IEEErem(1.0), 0.0)
        assertTrue("Incorrect remainder returned",
                1.32.IEEErem(89.765) >= 1.4705063220631647E-2 || 1.32.IEEErem(89.765) >= 1.4705063220631649E-2)
    }

    /**
     * Tests kotlin.math.ln(Double)
     */
    @Test fun lnD() {
        // Test for log(Double): Double
        var d = 10.0
        while (d >= -10) {
            val answer = ln(exp(d))
            assertTrue("Answer does not equal expected answer for d = " + d
                    + " answer = " + answer, abs(answer - d) <= abs(d * 0.00000001))
            d -= 0.5
        }
    }

    /**
     * Tests kotlin.math.log10(Double)
     */
    @Test fun log10_D() {
        // Test for special cases
        assertTrue(Double.isNaN(log10(Double.NaN)))
        assertTrue(Double.isNaN(log10(-2541.05745687234187532)))
        assertTrue(Double.isNaN(log10(-0.1)))
        assertEquals(Double.POSITIVE_INFINITY, log10(Double.POSITIVE_INFINITY))
        assertEquals(Double.NEGATIVE_INFINITY, log10(0.0))
        assertEquals(Double.NEGATIVE_INFINITY, log10(+0.0))
        assertEquals(Double.NEGATIVE_INFINITY, log10(-0.0))

        assertEquals(3.0, log10(1000.0))
        assertEquals(14.0, log10(10.0.pow(14.0)))
        assertEquals(3.7389561269540406, log10(5482.2158))
        assertEquals(14.661551142893833, log10(458723662312872.125782332587))
        assertEquals(-0.9083828622192334, log10(0.12348583358871))
        assertEquals(308.25471555991675, log10(Double.MAX_VALUE))
        assertEquals(-323.3062153431158, log10(Double.MIN_VALUE))
    }

    /**
     * Tests kotlin.math.ln1p(Double)
     */
    @Test fun ln1p_D() {
        // Test for special cases
        assertTrue("Should return NaN", Double.isNaN(ln1p(Double.NaN)))
        assertTrue("Should return NaN", Double.isNaN(ln1p(-32.0482175)))
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, ln1p(Double.POSITIVE_INFINITY), 0.0)
        assertEquals(0.0.toBits(), ln1p(0.0).toBits())
        assertEquals(+0.0.toBits(), ln1p(+0.0).toBits())
        assertEquals((-0.0).toBits(), ln1p(-0.0).toBits())

        assertEquals("Should return -0.2941782295312541", -0.2941782295312541,
                ln1p(-0.254856327), 0.0)
        assertEquals("Should return 7.368050685564151", 7.368050685564151, ln1p(1583.542), 0.0)
        assertEquals("Should return 0.4633708685409921", 0.4633708685409921,
                ln1p(0.5894227), 0.0)
        assertEquals("Should return 709.782712893384", 709.782712893384, ln1p(Double.MAX_VALUE), 0.0)
        assertEquals("Should return Double.MIN_VALUE", Double.MIN_VALUE, ln1p(Double.MIN_VALUE), 0.0)
    }

    /**
     * Tests kotlin.math.max(Double, Double)
     */
    @Test fun maxDD() {
        // Test for max(Double, Double): Double
        assertEquals("Incorrect Double max value", 1908897.6000089, max(-1908897.6000089,
                1908897.6000089), 0.0)
        assertEquals("Incorrect Double max value",
                1908897.6000089, max(2.0, 1908897.6000089), 0.0)
        assertEquals("Incorrect Double max value", -2.0, max(-2.0,
                -1908897.6000089), 0.0)

        // Compare toString representations here since -0.0 = +0.0, and
        // NaN != NaN and we need to distinguish
        assertEquals((Double.NaN).toString(), max(Double.NaN, 42.0).toString(), "Max failed for NaN")
        assertEquals((Double.NaN).toString(), max(42.0, Double.NaN).toString(), "Max failed for NaN")
        assertEquals((+0.0).toString(), max(+0.0, -0.0).toString(), "Max failed for 0.0")
        assertEquals((+0.0).toString(), max(-0.0, +0.0).toString(), "Max failed for 0.0")
        assertEquals((-0.0).toString(), max(-0.0, -0.0).toString(), "Max failed for -0.0d")
        assertEquals((+0.0).toString(), max(+0.0, +0.0).toString(), "Max failed for 0.0")
    }

    /**
     * Tests kotlin.math.max(float, float)
     */
    @Test fun maxFF() {
        // Test for max(float, float): float
        assertTrue("Incorrect float max value", max(-1908897.600f,
                1908897.600f) == 1908897.600f)
        assertTrue("Incorrect float max value",
                max(2.0f, 1908897.600f) == 1908897.600f)
        assertTrue("Incorrect float max value",
                max(-2.0f, -1908897.600f) == -2.0f)

        // Compare toString representations here since -0.0 = +0.0, and
        // NaN != NaN and we need to distinguish
        assertEquals(Float.NaN.toString(), max(Float.NaN, 42.0f).toString(), "Max failed for NaN")
        assertEquals(Float.NaN.toString(), max(42.0f, Float.NaN).toString(), "Max failed for NaN")
        assertEquals((+0.0f).toString(), max(+0.0f, -0.0f).toString(), "Max failed for 0.0")
        assertEquals((+0.0f).toString(), max(-0.0f, +0.0f).toString(), "Max failed for 0.0")
        assertEquals((-0.0f).toString(), max(-0.0f, -0.0f).toString(), "Max failed for -0.0f")
        assertEquals((+0.0f).toString(), max(+0.0f, +0.0f).toString(), "Max failed for 0.0")
    }

    /**
     * Tests kotlin.math.max(int, int)
     */
    @Test fun maxII() {
        // Test for max(int, int): int
        assertEquals("Incorrect int max value",
                19088976, max(-19088976, 19088976))
        assertEquals("Incorrect int max value",
                19088976, max(20, 19088976))
        assertEquals("Incorrect int max value", -20, max(-20, -19088976))
    }

    /**
     * Tests kotlin.math.max(long, long)
     */
    @Test fun maxJJ() {
        // Test for max(long, long): long
        assertEquals("Incorrect long max value", 19088976000089L, max(-19088976000089L,
                19088976000089L))
        assertEquals("Incorrect long max value",
                19088976000089L, max(20, 19088976000089L))
        assertEquals("Incorrect long max value",
                -20, max(-20, -19088976000089L))
    }

    /**
     * Tests kotlin.math.min(Double, Double)
     */
    @Test fun minDD() {
        // Test for min(Double, Double): Double
        assertEquals("Incorrect Double min value", -1908897.6000089, min(-1908897.6000089,
                1908897.6000089), 0.0)
        assertEquals("Incorrect Double min value",
                2.0, min(2.0, 1908897.6000089), 0.0)
        assertEquals("Incorrect Double min value", -1908897.6000089, min(-2.0,
                -1908897.6000089), 0.0)
        assertEquals("Incorrect Double min value", 1.0, min(1.0, 1.0))

        // Compare toString representations here since -0.0 = +0.0, and
        // NaN != NaN and we need to distinguish
        assertEquals(Double.NaN.toString(), min(Double.NaN, 42.0).toString(), "Min failed for NaN")
        assertEquals(Double.NaN.toString(), min(42.0, Double.NaN).toString(), "Min failed for NaN")
        assertEquals((-0.0).toString(), min(+0.0, -0.0).toString(), "Min failed for -0.0")
        assertEquals((-0.0).toString(), min(-0.0, +0.0).toString(), "Min failed for -0.0")
        assertEquals((-0.0).toString(), min(-0.0, -0.0).toString(), "Min failed for -0.0d")
        assertEquals((+0.0).toString(), min(+0.0, +0.0).toString(), "Min failed for 0.0")
    }

    /**
     * Tests kotlin.math.min(float, float)
     */
    @Test fun minFF() {
        // Test for min(float, float): float
        assertTrue("Incorrect float min value", min(-1908897.600f,
                1908897.600f) == -1908897.600f)
        assertTrue("Incorrect float min value",
                min(2.0f, 1908897.600f) == 2.0f)
        assertTrue("Incorrect float min value",
                min(-2.0f, -1908897.600f) == -1908897.600f)
        assertEquals("Incorrect float min value", 1.0f, min(1.0f, 1.0f))

        // Compare toString representations here since -0.0 = +0.0, and
        // NaN != NaN and we need to distinguish
        assertEquals(Float.NaN.toString(), min(Float.NaN, 42.0f).toString(), "Min failed for NaN")
        assertEquals(Float.NaN.toString(), min(42.0f, Float.NaN).toString(), "Min failed for NaN")
        assertEquals((-0.0f).toString(), min(+0.0f, -0.0f).toString(), "Min failed for -0.0")
        assertEquals((-0.0f).toString(), min(-0.0f, +0.0f).toString(), "Min failed for -0.0")
        assertEquals((-0.0f).toString(), min(-0.0f, -0.0f).toString(), "Min failed for -0.0f")
        assertEquals((+0.0f).toString(), min(+0.0f, +0.0f).toString(), "Min failed for 0.0")
    }

    /**
     * Tests kotlin.math.min(int, int)
     */
    @Test fun minII() {
        // Test for min(int, int): int
        assertEquals("Incorrect int min value",
                -19088976, min(-19088976, 19088976))
        assertEquals("Incorrect int min value", 20, min(20, 19088976))
        assertEquals("Incorrect int min value",
                -19088976, min(-20, -19088976))

    }

    /**
     * @tests java.lang.Math#pow(double, double)
     */
    fun test_powDD() {
        // Test for method double java.lang.Math.pow(double, double)
        assertTrue("pow returned incorrect value",
                2.0.pow(8.0).toLong() == 256L)
        assertTrue("pow returned incorrect value",
                2.0.pow(-8.0) == 0.00390625)
        assertEquals("Incorrect root returned1",
                2.0, sqrt(sqrt(2.0).pow(4.0)), 0.0)
    }

    /**
     * Tests kotlin.math.round(Double)
     */
    @Test fun roundD() {
        // Test for round(Double): Double
        assertEquals("Failed to round properly - up to odd",
                3.0, round(2.9), 0.0)
        assertTrue("Failed to round properly - NaN", Double.isNaN(round(Double.NaN)))
        assertEquals("Failed to round properly down  to even",
                2.0, round(2.1), 0.0)
        assertTrue("Failed to round properly " + 2.5 + " to even", round(2.5) == 2.0)
        assertTrue("Failed to round properly " + +0.0,
                round(+0.0) == +0.0)
        assertTrue("Failed to round properly " + -0.0,
                round(-0.0) == -0.0)
    }

    /**
     * Tests kotlin.math.sign(Double)
     */
    @Test fun sign_D() {
        assertTrue(Double.isNaN(sign(Double.NaN)))
        assertTrue(Double.isNaN(sign(Double.NaN)))
        assertEquals(0.0.toBits(), sign(0.0).toBits())
        assertEquals(+0.0.toBits(), sign(+0.0).toBits())
        assertEquals((-0.0).toBits(), sign(-0.0).toBits())

        assertEquals(1.0, sign(253681.2187962), 0.0)
        assertEquals(-1.0, sign(-125874693.56), 0.0)
        assertEquals(1.0, sign(1.2587E-308), 0.0)
        assertEquals(-1.0, sign(-1.2587E-308), 0.0)

        assertEquals(1.0, sign(Double.MAX_VALUE), 0.0)
        assertEquals(1.0, sign(Double.MIN_VALUE), 0.0)
        assertEquals(-1.0, sign(-Double.MAX_VALUE), 0.0)
        assertEquals(-1.0, sign(-Double.MIN_VALUE), 0.0)
        assertEquals(1.0, sign(Double.POSITIVE_INFINITY), 0.0)
        assertEquals(-1.0, sign(Double.NEGATIVE_INFINITY), 0.0)
    }

    /**
     * Tests kotlin.math.sign(float)
     */
    @Test fun sign_F() {
        assertTrue(Float.isNaN(sign(Float.NaN)))
        assertEquals(0.0f.toBits(), sign(0.0f).toBits())
        assertEquals(+0.0f.toBits(), sign(+0.0f).toBits())
        assertEquals((-0.0f).toBits(), sign(-0.0f).toBits())

        assertEquals(1.0f, sign(253681.2187962f), 0f)
        assertEquals(-1.0f, sign(-125874693.56f), 0f)
        assertEquals(1.0f, sign(1.2587E-11f), 0f)
        assertEquals(-1.0f, sign(-1.2587E-11f), 0f)

        assertEquals(1.0f, sign(Float.MAX_VALUE), 0f)
        assertEquals(1.0f, sign(Float.MIN_VALUE), 0f)
        assertEquals(-1.0f, sign(-Float.MAX_VALUE), 0f)
        assertEquals(-1.0f, sign(-Float.MIN_VALUE), 0f)
        assertEquals(1.0f, sign(Float.POSITIVE_INFINITY), 0f)
        assertEquals(-1.0f, sign(Float.NEGATIVE_INFINITY), 0f)
    }

    /**
     * Tests kotlin.math.sin(Double)
     */
    @Test fun sinD() {
        // Test for sin(Double): Double
        assertEquals("Incorrect answer", 0.0, sin(0.0), 0.0)
        assertEquals("Incorrect answer", 0.8414709848078965, sin(1.0), 0.0)
    }

    /**
     * Tests kotlin.math.sinh(Double)
     */
    @Test fun sinh_D() {
        // Test for special situations
        assertTrue("Should return NaN", Double.isNaN(sinh(Double.NaN)))
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, sinh(Double.POSITIVE_INFINITY), 0.0)
        assertEquals("Should return NEGATIVE_INFINITY",
                Double.NEGATIVE_INFINITY, sinh(Double.NEGATIVE_INFINITY), 0.0)
        assertEquals(0.0.toBits(), sinh(0.0).toBits())
        assertEquals(+0.0.toBits(), sinh(+0.0).toBits())
        assertEquals((-0.0).toBits(), sinh(-0.0).toBits())

        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, sinh(1234.56), 0.0)
        assertEquals("Should return NEGATIVE_INFINITY",
                Double.NEGATIVE_INFINITY, sinh(-1234.56), 0.0)
        assertEquals("Should return 1.0000000000001666E-6",
                1.0000000000001666E-6, sinh(0.000001), 0.0)
        assertEquals("Should return -1.0000000000001666E-6",
                -1.0000000000001666E-6, sinh(-0.000001), 0.0)
        assertEquals("Should return 5.115386441963859", 5.115386441963859, sinh(2.33482))
        assertEquals("Should return POSITIVE_INFINITY",
                Double.POSITIVE_INFINITY, sinh(Double.MAX_VALUE), 0.0)
        assertEquals("Should return 4.9E-324", 4.9E-324, sinh(Double.MIN_VALUE), 0.0)
    }

    /**
     * Tests kotlin.math.sqrt(Double)
     */
    @Test fun sqrt_D() {
        // Test for sqrt(Double): Double
        assertEquals("Incorrect root returned2", 7.0, sqrt(49.0), 0.0)
    }

    /**
     * Tests kotlin.math.tan(Double)
     */
    @Test fun tan_D() {
        // Test for tan(Double): Double
        assertEquals("Incorrect answer", 0.0, tan(0.0), 0.0)
        assertEquals("Incorrect answer", 1.5574077246549023, tan(1.0))

    }

    /**
     * Tests kotlin.math.tanh(Double)
     */
    @Test fun tanh_D() {
        // Test for special situations
        assertTrue("Should return NaN", Double.isNaN(tanh(Double.NaN)))
        assertEquals("Should return +1.0", +1.0, tanh(Double.POSITIVE_INFINITY), 0.0)
        assertEquals("Should return -1.0", -1.0, tanh(Double.NEGATIVE_INFINITY), 0.0)
        assertEquals(0.0.toBits(), tanh(0.0).toBits())
        assertEquals(+0.0.toBits(), tanh(+0.0).toBits())
        assertEquals((-0.0).toBits(), tanh(-0.0).toBits())

        assertEquals("Should return 1.0", 1.0, tanh(1234.56), 0.0)
        assertEquals("Should return -1.0", -1.0, tanh(-1234.56), 0.0)
        assertEquals("Should return 9.999999999996666E-7",
                9.999999999996666E-7, tanh(0.000001), 0.0)
        assertEquals("Should return 0.981422884124941", 0.981422884124941, tanh(2.33482), 0.0)
        assertEquals("Should return 1.0", 1.0, tanh(Double.MAX_VALUE), 0.0)
        assertEquals("Should return 4.9E-324", 4.9E-324, tanh(Double.MIN_VALUE), 0.0)
    }

    /**
     * Tests kotlin.Double.ulp
     */
    fun test_ulp_D() {
        // Test for special cases
        assertTrue("Should return NaN", Double.isNaN(ulp(Double.NaN)))
        assertEquals("Returned incorrect value", Double.POSITIVE_INFINITY, ulp(Double.POSITIVE_INFINITY), 0.0)
        assertEquals("Returned incorrect value", Double.POSITIVE_INFINITY, ulp(Double.NEGATIVE_INFINITY), 0.0)
        assertEquals("Returned incorrect value", Double.MIN_VALUE, ulp(0.0), 0.0)
        assertEquals("Returned incorrect value", Double.MIN_VALUE, ulp(+0.0), 0.0)
        assertEquals("Returned incorrect value", Double.MIN_VALUE, ulp(-0.0), 0.0)
        assertEquals("Returned incorrect value", pow(2.0, 971.0), ulp(Double.MAX_VALUE), 0.0)
        assertEquals("Returned incorrect value", pow(2.0, 971.0), ulp(-Double.MAX_VALUE), 0.0)

        assertEquals("Returned incorrect value", Double.MIN_VALUE, ulp(Double.MIN_VALUE), 0.0)
        assertEquals("Returned incorrect value", Double.MIN_VALUE, ulp(-Double.MIN_VALUE), 0.0)

        assertEquals("Returned incorrect value", 2.220446049250313E-16, ulp(1.0), 0.0)
        assertEquals("Returned incorrect value", 2.220446049250313E-16, ulp(-1.0), 0.0)
        assertEquals("Returned incorrect value", 2.2737367544323206E-13, ulp(1153.0), 0.0)
    }

    /**
     * Tests kotlin.Float.ulp
     */
    fun test_ulp_f() {
        // Test for special cases
        assertTrue("Should return NaN", Float.isNaN(ulp(Float.NaN)))
        assertEquals("Returned incorrect value", Float.POSITIVE_INFINITY, ulp(Float.POSITIVE_INFINITY), 0f)
        assertEquals("Returned incorrect value", Float.POSITIVE_INFINITY, ulp(Float.NEGATIVE_INFINITY), 0f)
        assertEquals("Returned incorrect value", Float.MIN_VALUE, ulp(0.0f), 0f)
        assertEquals("Returned incorrect value", Float.MIN_VALUE, ulp(+0.0f), 0f)
        assertEquals("Returned incorrect value", Float.MIN_VALUE, ulp(-0.0f), 0f)
        assertEquals("Returned incorrect value", 2.028241E31f, ulp(Float.MAX_VALUE), 0f)
        assertEquals("Returned incorrect value", 2.028241E31f, ulp(-Float.MAX_VALUE), 0f)

        assertEquals("Returned incorrect value", 1.4E-45f, ulp(Float.MIN_VALUE), 0f)
        assertEquals("Returned incorrect value", 1.4E-45f, ulp(-Float.MIN_VALUE), 0f)

        assertEquals("Returned incorrect value", 1.1920929E-7f, ulp(1.0f),
                0f)
        assertEquals("Returned incorrect value", 1.1920929E-7f,
                ulp(-1.0f), 0f)
        assertEquals("Returned incorrect value", 1.2207031E-4f, ulp(1153.0f), 0f)
        assertEquals("Returned incorrect value", 5.6E-45f, ulp(9.403954E-38f), 0f)
    }

    companion object {

        const val MIN_NORMAL_D: Double = 2.2250738585072014E-308
        const val MIN_NORMAL_F: Float  = 1.1754943508222875E-38f

        /**
         * cases for test_copySign_DD in est/Strictest
         */
        internal val COPYSIGN_DD_CASES = doubleArrayOf(Double.POSITIVE_INFINITY, Double.MAX_VALUE, 3.4E302, 2.3,
                MIN_NORMAL_D, MIN_NORMAL_D / 2, Double.MIN_VALUE, +0.0, 0.0, -0.0, -Double.MIN_VALUE,
                -MIN_NORMAL_D / 2, -MIN_NORMAL_D, -4.5, -3.4E102, -Double.MAX_VALUE, Double.NEGATIVE_INFINITY)

        /**
         * cases for test_copySign_FF in est/Strictest
         */
        internal val COPYSIGN_FF_CASES = floatArrayOf(Float.POSITIVE_INFINITY, Float.MAX_VALUE, 3.4E12f, 2.3f,
                MIN_NORMAL_F, MIN_NORMAL_F / 2, Float.MIN_VALUE, +0.0f, 0.0f, -0.0f, -Float.MIN_VALUE,
                -MIN_NORMAL_F / 2, -MIN_NORMAL_F, -4.5f, -5.6442E21f, -Float.MAX_VALUE, Float.NEGATIVE_INFINITY)

        /**
         * start number cases for test_nextTowards_DD in est/Strictest
         * NEXTAFTER_DD_START_CASES[i][0] is the start number
         * NEXTAFTER_DD_START_CASES[i][1] is the nextUp of start number
         * NEXTAFTER_DD_START_CASES[i][2] is the nextDown of start number
         */
        internal val NEXTAFTER_DD_START_CASES = arrayOf(
                doubleArrayOf(3.4, 3.4000000000000004, 3.3999999999999995),
                doubleArrayOf(-3.4, -3.3999999999999995, -3.4000000000000004),
                doubleArrayOf(3.4233E109, 3.4233000000000005E109, 3.4232999999999996E109),
                doubleArrayOf(-3.4233E109, -3.4232999999999996E109, -3.4233000000000005E109),
                doubleArrayOf(+0.0, Double.MIN_VALUE, -Double.MIN_VALUE),
                doubleArrayOf(0.0, Double.MIN_VALUE, -Double.MIN_VALUE),
                doubleArrayOf(-0.0, Double.MIN_VALUE, -Double.MIN_VALUE),
                doubleArrayOf(Double.MIN_VALUE, 1.0E-323, +0.0),
                doubleArrayOf(-Double.MIN_VALUE, -0.0, -1.0E-323),
                doubleArrayOf(MIN_NORMAL_D, 2.225073858507202E-308, 2.225073858507201E-308),
                doubleArrayOf(-MIN_NORMAL_D, -2.225073858507201E-308, -2.225073858507202E-308),
                doubleArrayOf(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 1.7976931348623155E308),
                doubleArrayOf(-Double.MAX_VALUE, -1.7976931348623155E308, Double.NEGATIVE_INFINITY),
                doubleArrayOf(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.MAX_VALUE),
                doubleArrayOf(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, Double.NEGATIVE_INFINITY)
        )

        /**
         * direction number cases for test_nextTowards_DD/test_nextTowards_FD in
         * est/Strictest
         */
        internal val NEXTAFTER_DD_FD_DIRECTION_CASES = doubleArrayOf(Double.POSITIVE_INFINITY,
                Double.MAX_VALUE, 8.8, 3.4, 1.4, MIN_NORMAL_D, MIN_NORMAL_D / 2,
                Double.MIN_VALUE, +0.0, 0.0, -0.0, -Double.MIN_VALUE, -MIN_NORMAL_D / 2,
                -MIN_NORMAL_D, -1.4, -3.4, -8.8, -Double.MAX_VALUE, Double.NEGATIVE_INFINITY)

        /**
         * start number cases for test_nextTowards_FD in est/Strictest
         * NEXTAFTER_FD_START_CASES[i][0] is the start number
         * NEXTAFTER_FD_START_CASES[i][1] is the nextUp of start number
         * NEXTAFTER_FD_START_CASES[i][2] is the nextDown of start number
         */
        internal val NEXTAFTER_FD_START_CASES = arrayOf(floatArrayOf(3.4f, 3.4000003f, 3.3999999f),
                floatArrayOf(-3.4f, -3.3999999f, -3.4000003f),
                floatArrayOf(3.4233E19f, 3.4233002E19f, 3.4232998E19f),
                floatArrayOf(-3.4233E19f, -3.4232998E19f, -3.4233002E19f),
                floatArrayOf(+0.0f, Float.MIN_VALUE, -Float.MIN_VALUE),
                floatArrayOf(0.0f, Float.MIN_VALUE, -Float.MIN_VALUE),
                floatArrayOf(-0.0f, Float.MIN_VALUE, -Float.MIN_VALUE),
                floatArrayOf(Float.MIN_VALUE, 2.8E-45f, +0.0f),
                floatArrayOf(-Float.MIN_VALUE, -0.0f, -2.8E-45f),
                floatArrayOf(MIN_NORMAL_F, 1.1754945E-38f, 1.1754942E-38f),
                floatArrayOf(-MIN_NORMAL_F, -1.1754942E-38f, -1.1754945E-38f),
                floatArrayOf(Float.MAX_VALUE, Float.POSITIVE_INFINITY, 3.4028233E38f),
                floatArrayOf(-Float.MAX_VALUE, -3.4028233E38f, Float.NEGATIVE_INFINITY),
                floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.MAX_VALUE),
                floatArrayOf(Float.NEGATIVE_INFINITY, -Float.MAX_VALUE, Float.NEGATIVE_INFINITY))
    }
}