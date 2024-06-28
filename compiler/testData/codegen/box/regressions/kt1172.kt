// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// not sure if it's ok to change Object to Any

// WITH_STDLIB

package test.regressions.kt1172

public fun scheduleRefresh(vararg files : Object) {
    ArrayList<Object>(files.map { it })
}

fun box(): String {
    scheduleRefresh()
    return "OK"
}
