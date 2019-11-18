// IGNORE_BACKEND_FIR: JVM_IR
val zint : Int? = 1
val zlong : Long? = 2
val zbyte : Byte? = 3
val zshort : Short? = 4
val zchar : Char? = 'c'
val zdouble : Double? = 1.0
val zfloat : Float? = 2.0.toFloat()

fun box(): String {
  return "OK"
}