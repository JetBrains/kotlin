// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FILE: JBase.java

public interface JBase extends Base {
    default String test() {
        return "OK";
    }
}

// FILE: main.kt

interface Base {
    fun test(): String = "Base"
}

interface LeftBase : Base
interface Right : JBase

interface Child : LeftBase, Right


fun box(): String {
    return object : Child {}.test()
}
