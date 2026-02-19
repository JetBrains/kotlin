// ISSUE: KT-67652
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect open class AbstractMutableList {
    var modCount: Int
}

// MODULE: jvm()()(common)
// FILE: jvm.kt
actual open class AbstractMutableList : Jaba() {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    actual var modCount: Int
        get() = 42
        set(value) { }

    fun added(): Int {
        return modCount
    }
}

fun box(): String {
    val result = AbstractMutableList().added()
    return if (result == 0) "OK" else "$result"
}

// FILE: Jaba.java
public class Jaba {
    public int modCount = 0;
}
