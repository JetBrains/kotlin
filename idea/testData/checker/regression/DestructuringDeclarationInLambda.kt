// FIR_IDENTICAL

data class XY(val x: Int, val y: Int)

fun convert(xy: XY, f: (XY) -> Int) = f(xy)

fun foo() = <error>convert</error> { (<error>x</error><error><error>,</error> y)</error> }
