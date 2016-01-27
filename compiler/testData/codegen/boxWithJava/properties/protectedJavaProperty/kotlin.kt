package z

import JavaBaseClass

object KotlinExtender : JavaBaseClass() {
    @JvmStatic fun test(): String {
        return runSlowly {
            foo = "OK"
            foo
        }
    }
}
fun runSlowly(f: () -> String): String {
    return f()
}

fun box(): String {
    return KotlinExtender.test()
}