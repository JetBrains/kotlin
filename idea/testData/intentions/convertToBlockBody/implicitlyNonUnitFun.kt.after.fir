// WITH_RUNTIME

fun foo(): String {
    return java.io.File("x").getAbsolutePath()
}