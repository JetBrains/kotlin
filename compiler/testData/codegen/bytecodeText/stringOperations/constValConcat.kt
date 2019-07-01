const val string = "2"
const val int = 3
const val long = 4L
const val double = 5.0
const val float = 6F
const val char = '7'

val s = "1" + string + int + long + double + float + char
val c = "1$string$int$long$double$float$char"

// 0 NEW java/lang/StringBuilder
// 2 LDC "12345.06.07"