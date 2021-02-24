// IGNORE_BACKEND: JVM
// KT-44631

class Something(val now: String)

fun box(): String {
    val a: Something.() -> String = {
        class MyEvent(val result: String = now)

        MyEvent().result
    }
    return Something("OK").a()
}
