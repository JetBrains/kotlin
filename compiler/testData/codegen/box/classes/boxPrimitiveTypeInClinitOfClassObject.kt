class A {
  default object {
    var xi = 0
    var xin : Int? = 0
    var xinn : Int? = null

    var xl = 0.toLong()
    var xln : Long? = 0.toLong()
    var xlnn : Long? = null

    var xb = 0.toByte()
    var xbn : Byte? = 0.toByte()
    var xbnn : Byte? = null

    var xf = 0.toFloat()
    var xfn : Float? = 0.toFloat()
    var xfnn : Float? = null

    var xd = 0.toDouble()
    var xdn : Double? = 0.toDouble()
    var xdnn : Double? = null

    var xs = 0.toShort()
    var xsn : Short? = 0.toShort()
    var xsnn : Short? = null
  }
}

fun box() : String {
    return "OK"
}
