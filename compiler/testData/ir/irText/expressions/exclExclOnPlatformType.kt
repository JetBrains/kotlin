// TARGET_BACKEND: JVM
// FULL_JDK

import java.lang.ref.WeakReference

fun use(a: Any) {}

fun test() {
    use(WeakReference("").get()!!)
    use(System.getProperty("abc")!!)
}
