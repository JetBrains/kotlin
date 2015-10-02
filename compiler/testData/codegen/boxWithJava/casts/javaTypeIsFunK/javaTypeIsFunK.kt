fun box(): String {
    val jfun = JFun()
    val jf = jfun as Any
    if (jf is Function0<*>) return jfun()
    else return "Failed: jf is Function0<*>"
}