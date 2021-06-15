// WITH_RUNTIME

fun foo(): Array<out String?>? {
    return java.io.File("x").list()
}