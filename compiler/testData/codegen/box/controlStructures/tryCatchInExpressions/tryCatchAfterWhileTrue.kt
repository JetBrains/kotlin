// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
fun box(): String {
    val a = arrayListOf<String>()

    while (true) {
        if (a.size == 0) {
            break
        }
    }

    for (i in 1..2) {
        a.add(try { foo() } catch (e: Throwable) { "OK" })
    }

    return a[0]
}

fun foo(): String = throw RuntimeException()
