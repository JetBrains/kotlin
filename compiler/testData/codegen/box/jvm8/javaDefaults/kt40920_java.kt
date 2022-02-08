// !JVM_DEFAULT_MODE: disable
// JVM_TARGET: 1.8
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: Base.java

public interface Base {
    default String test() {
        return "Base";
    }
}


// FILE: main.kt

interface LeftBase : Base
interface Right : Base {
    override fun test(): String = "OK"
}

interface Child : LeftBase, Right


fun box(): String {
    return object : Child {}.test()
}
