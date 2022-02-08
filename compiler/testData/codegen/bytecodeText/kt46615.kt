// TARGET_BACKEND: JVM_IR

fun box() {
    val str = "OK"
    val a = { s: String -> s }("OK")
    val b = { s: String -> s }(str)
    val c = { s: String -> s }
    c.invoke("OK")
}

// 1 checkNotNullParameter