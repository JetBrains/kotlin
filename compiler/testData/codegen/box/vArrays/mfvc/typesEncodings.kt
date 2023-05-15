// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(
    val bool: Boolean,
    val byte: Byte,
    val short: Short,
    val char: Char,
    val int: Int,
    val float: Float,
    val long: Long,
    val double: Double,
    val string: String
)

@JvmInline
value class X(val z: String, val x: VArray<Point>)

fun box(): String {

    val vArray = VArray(9) {
        Point(
            it > 0,
            it.toByte(),
            it.toShort(),
            '0' + it,
            it,
            it.toFloat(),
            it.toLong(),
            it.toDouble(),
            it.toString()
        )
    }

    val bool = vArray[0].bool
    val byte = vArray[1].byte
    val short = vArray[2].short
    val char = vArray[3].char
    val int = vArray[4].int
    val float = vArray[5].float
    val long = vArray[6].long
    val double = vArray[7].double
    val string = vArray[8].string

    val str = "$bool $byte $short $char $int $float $long $double $string"

    if (str != "false 1 2 3 4 5.0 6 7.0 8") return "Fail"

    return "OK"
}