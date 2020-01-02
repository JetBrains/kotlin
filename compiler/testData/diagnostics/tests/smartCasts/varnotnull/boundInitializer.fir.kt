fun foo(arg: Int?) {
    val x = arg
    if (x != null) {
        arg.hashCode()
    }
    val y: Any? = arg
    if (y != null) {
        arg.hashCode()
    }
    val yy: Any?
    yy = arg
    if (yy != null) {
        arg.hashCode()
    }
    var z = arg
    z = z?.let { 42 }
    if (z != null) {
        arg.hashCode()
    }
}
