class C {
  var myIntProp: Int = 1
  var myByteProp: Byte = 2
  var myLongProp: Long = 3L
  var myShortProp: Short = 4
  var myDoubleProp: Double = 5.6
  var myFloatProp: Float = 7.8f
  var myBooleanProp: Boolean = true
  var myCharProp: Char = '9'

  init {
    myIntProp = 0
    myByteProp = 0
    myLongProp = 0L
    myShortProp = 0
    myDoubleProp = 0.0
    myFloatProp = 0.0f
    myBooleanProp = false
    myCharProp = '\u0000'
  }
}

fun box(): String {
  val c = C()
  if (c.myIntProp != 0) return "fail Int"
  if (c.myByteProp != 0.toByte()) return "fail Byte"
  if (c.myLongProp != 0L) return "fail Long"
  if (c.myShortProp != 0.toShort()) return "fail Short"
  if (c.myDoubleProp != 0.0) return "fail Double"
  if (c.myFloatProp != 0.0f) return "fail Float"
  if (c.myBooleanProp != false) return "fail Boolean"
  if (c.myCharProp != '\u0000') return "fail Char"
  return "OK"
}
