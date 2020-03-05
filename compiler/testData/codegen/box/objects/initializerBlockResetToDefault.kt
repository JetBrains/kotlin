object C {
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
  if (C.myIntProp != 0) return "fail Int"
  if (C.myByteProp != 0.toByte()) return "fail Byte"
  if (C.myLongProp != 0L) return "fail Long"
  if (C.myShortProp != 0.toShort()) return "fail Short"
  if (C.myDoubleProp != 0.0) return "fail Double"
  if (C.myFloatProp != 0.0f) return "fail Float"
  if (C.myBooleanProp != false) return "fail Boolean"
  if (C.myCharProp != '\u0000') return "fail Char"
  return "OK"
}
