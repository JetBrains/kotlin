fun box(): String {
    return JavaClass().doIt()
}

internal fun JavaClass.doIt(): String {
    x = "OK"
    return x
}
