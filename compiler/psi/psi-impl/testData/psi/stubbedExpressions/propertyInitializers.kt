// A property initializer is stubbed as long as the expression itself is stub-based
package test

const val boolean: Boolean = true
const val byte: Byte = 1
const val short: Short = 2
const val int: Int = 3
const val long: Long = 4L
const val float: Float = 5.0F
const val double: Double = 6.6
const val char: Char = 'c'
const val string: String = "s"

const val negative: Int = -7
const val sum: Int = 1 + 2
const val parenthesized: Int = (8)
const val concatenation: String = "a" + "b"

const val reference: Int = int
const val qualified: Int = test.int

// A special floating point value has no literal form, so it stays a reference outside the class that declares it
const val infinity: Double = Double.POSITIVE_INFINITY
const val nan: Float = Float.NaN

// A constant may be computed by an intrinsic call, so the source has a call where the metadata has a value
const val intrinsicCall: String = 12.toString()
const val intrinsicProperty: Int = "abc".length

val nonConst: Int = 9
var mutable: String = "m"

object Holder {
    const val inObject: Int = 10
    val inferred = 11
    const val infinityInObject: Double = Double.POSITIVE_INFINITY
}

class WithMember {
    val member: Char = 'm'
}
