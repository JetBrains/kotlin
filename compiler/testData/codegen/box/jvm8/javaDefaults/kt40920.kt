// !JVM_DEFAULT_MODE: disable
// JVM_TARGET: 1.8
// TARGET_BACKEND: JVM
// WITH_STDLIB
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
