// !JVM_DEFAULT_MODE: disable
// JVM_TARGET: 1.8
// TARGET_BACKEND: JVM
// FILE: Base.java

public interface Base {
    default String test() {
        return "Base";
    }
}

// FILE: Left.java

public interface Left extends Base {
}

// FILE: main.kt
// WITH_RUNTIME

interface Right : Base {
    override fun test(): String = "OK"
}

interface Child : Left, Right

fun box(): String {
    return object : Child {}.test()
}
