// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// FILE: JBase.java

public interface JBase extends Base {
    default String test() {
        return "OK";
    }
}

// FILE: main.kt
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Base {
    @JvmDefault
    fun test(): String = "Base"
}

interface LeftBase : Base
interface Right : JBase

interface Child : LeftBase, Right


fun box(): String {
    return object : Child {}.test()
}
