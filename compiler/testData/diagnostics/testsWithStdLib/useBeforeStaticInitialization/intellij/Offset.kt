// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// RENDER_DIAGNOSTIC_ARGUMENTS
// WITH_STDLIB

// FILE: Offset.kt

/** Linearly interpolate between [start] and [stop] with [fraction] fraction between them. */
fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

/** Linearly interpolate between [start] and [stop] with [fraction] fraction between them. */
fun lerp(start: Int, stop: Int, fraction: Float): Int {
    return start + ((stop - start) * fraction.toDouble()).toInt()
}

/** Linearly interpolate between [start] and [stop] with [fraction] fraction between them. */
fun lerp(start: Long, stop: Long, fraction: Float): Long {
    return start + ((stop - start) * fraction.toDouble()).toLong()
}

/** Packs two Float values into one Long value for use in inline classes. */
fun packFloats(val1: Float, val2: Float): Long {
    val v1 = val1.toBits().toLong()
    val v2 = val2.toBits().toLong()
    return v1.shl(32) or (v2 and 0xFFFFFFFF)
}

/** Unpacks the first Float value in [packFloats] from its returned Long. */
fun unpackFloat1(value: Long): Float {
    return Float.fromBits(value.shr(32).toInt())
}

/** Unpacks the second Float value in [packFloats] from its returned Long. */
fun unpackFloat2(value: Long): Float {
    return Float.fromBits(value.and(0xFFFFFFFF).toInt())
}

/** Packs two Int values into one Long value for use in inline classes. */
fun packInts(val1: Int, val2: Int): Long {
    return val1.toLong().shl(32) or (val2.toLong() and 0xFFFFFFFF)
}

/** Unpacks the first Int value in [packInts] from its returned ULong. */
fun unpackInt1(value: Long): Int {
    return value.shr(32).toInt()
}

/** Unpacks the second Int value in [packInts] from its returned ULong. */
fun unpackInt2(value: Long): Int {
    return value.and(0xFFFFFFFF).toInt()
}

/** Constructs an Offset from the given relative x and y offsets */
fun Offset(x: Float, y: Float) = Offset(packFloats(x, y))

/**
 * An immutable 2D floating-point offset.
 *
 * Generally speaking, Offsets can be interpreted in two ways:
 * 1. As representing a point in Cartesian space a specified distance from a separately-maintained
 *    origin. For example, the top-left position of children in the [RenderBox] protocol is
 *    typically represented as an [Offset] from the top left of the parent box.
 * 2. As a vector that can be applied to coordinates. For example, when painting a [RenderObject],
 *    the parent is passed an [Offset] from the screen's origin which it can add to the offsets of
 *    its children to find the [Offset] from the screen's origin to each of the children.
 *
 * Because a particular [Offset] can be interpreted as one sense at one time then as the other sense
 * at a later time, the same class is used for both senses.
 *
 * See also:
 * * [Size], which represents a vector describing the size of a rectangle.
 *
 * Creates an offset. The first argument sets [x], the horizontal component, and the second sets
 * [y], the vertical component.
 */
@kotlin.jvm.JvmInline
value class Offset internal constructor(internal val packedValue: Long) {

    val x: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) { "Offset is unspecified" }
            return unpackFloat1(packedValue)
        }

    val y: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) { "Offset is unspecified" }
            return unpackFloat2(packedValue)
        }

    operator fun component1(): Float = x

    operator fun component2(): Float = y

    /** Returns a copy of this Offset instance optionally overriding the x or y parameter */
    fun copy(x: Float = this.x, y: Float = this.y) = Offset(x, y)

    companion object {
        /**
         * An offset with zero magnitude.
         *
         * This can be used to represent the origin of a coordinate space.
         */
        val Zero = Offset(0.0f, 0.0f)

        /**
         * An offset with infinite x and y components.
         *
         * See also:
         * * [isInfinite], which checks whether either component is infinite.
         * * [isFinite], which checks whether both components are finite.
         */
        // This is included for completeness, because [Size.infinite] exists.
        val Infinite = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)

        /**
         * Represents an unspecified [Offset] value, usually a replacement for `null` when a
         * primitive value is desired.
         */
        val Unspecified = Offset(Float.NaN, Float.NaN)
    }

    fun isValid(): Boolean {
        check(!x.isNaN() && !y.isNaN()) { "Offset argument contained a NaN value." }
        return true
    }

    /**
     * The square of the magnitude of the offset.
     *
     * This is cheaper than computing the [getDistance] itself.
     */
    fun getDistanceSquared() = x * x + y * y

    /**
     * Unary negation operator.
     *
     * Returns an offset with the coordinates negated.
     *
     * If the [Offset] represents an arrow on a plane, this operator returns the same arrow but
     * pointing in the reverse direction.
     */
    operator fun unaryMinus(): Offset = Offset(-x, -y)

    /**
     * Binary subtraction operator.
     *
     * Returns an offset whose [x] value is the left-hand-side operand's [x] minus the
     * right-hand-side operand's [x] and whose [y] value is the left-hand-side operand's [y] minus
     * the right-hand-side operand's [y].
     */
    operator fun minus(other: Offset): Offset = Offset(x - other.x, y - other.y)

    /**
     * Binary addition operator.
     *
     * Returns an offset whose [x] value is the sum of the [x] values of the two operands, and whose
     * [y] value is the sum of the [y] values of the two operands.
     */
    operator fun plus(other: Offset): Offset = Offset(x + other.x, y + other.y)

    /**
     * Multiplication operator.
     *
     * Returns an offset whose coordinates are the coordinates of the left-hand-side operand (an
     * Offset) multiplied by the scalar right-hand-side operand (a Float).
     */
    operator fun times(operand: Float): Offset = Offset(x * operand, y * operand)

    /**
     * Division operator.
     *
     * Returns an offset whose coordinates are the coordinates of the left-hand-side operand (an
     * Offset) divided by the scalar right-hand-side operand (a Float).
     */
    operator fun div(operand: Float): Offset = Offset(x / operand, y / operand)

    /**
     * Modulo (remainder) operator.
     *
     * Returns an offset whose coordinates are the remainder of dividing the coordinates of the
     * left-hand-side operand (an Offset) by the scalar right-hand-side operand (a Float).
     */
    operator fun rem(operand: Float) = Offset(x % operand, y % operand)
}

/**
 * Linearly interpolate between two offsets.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning that the
 * interpolation has not started, returning [start] (or something equivalent to [start]), 1.0
 * meaning that the interpolation has finished, returning [stop] (or something equivalent to
 * [stop]), and values in between meaning that the interpolation is at the relevant point on the
 * timeline between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and 1.0, so
 * negative values and values greater than 1.0 are valid (and can easily be generated by curves).
 *
 * Values for [fraction] are usually obtained from an [Animation<Float>], such as an
 * `AnimationController`.
 */

fun lerp(start: Offset, stop: Offset, fraction: Float): Offset {
    return Offset(lerp(start.x, stop.x, fraction), lerp(start.y, stop.y, fraction))
}

/** True if both x and y values of the [Offset] are finite */
val Offset.isFinite: Boolean
    get() = x.isFinite() && y.isFinite()

/** `false` when this is [Offset.Unspecified]. */
val Offset.isSpecified: Boolean
    get() = packedValue != Offset.Unspecified.packedValue

/** `true` when this is [Offset.Unspecified]. */
val Offset.isUnspecified: Boolean
    get() = packedValue == Offset.Unspecified.packedValue

/**
 * If this [Offset]&nbsp;[isSpecified] then this is returned, otherwise [block] is executed and its
 * result is returned.
 */
inline fun Offset.takeOrElse(block: () -> Offset): Offset = if (isSpecified) this else block()

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, companionObject, equalityExpression,
funWithExtensionReceiver, functionDeclaration, functionalType, getter, ifExpression, inline, integerLiteral,
lambdaLiteral, localProperty, multiplicativeExpression, objectDeclaration, operator, primaryConstructor,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral, thisExpression, unaryExpression, value */
