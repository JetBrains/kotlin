// !JVM_DEFAULT_MODE: disable
// JVM_TARGET: 1.8
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: JBase.java

public interface JBase extends Base {
    default String test() {
        return "OK";
    }
}

// FILE: main.kt
// WITH_RUNTIME

interface Base {
    fun test(): String = "Base"
}

interface LeftBase : Base
interface Right : JBase

interface Child : LeftBase, Right


fun box(): String {
    return object : Child {}.test()
}
