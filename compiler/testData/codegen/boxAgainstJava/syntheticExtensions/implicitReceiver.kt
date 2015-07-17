fun box(): String {
    return JavaClass().doIt()
}

fun JavaClass.doIt(): String {
    x = "OK"
    return x
}
