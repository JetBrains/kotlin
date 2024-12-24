// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-67652
// LANGUAGE: +MultiPlatformProjects
// FIR_DUMP

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

    fun added() {
        val x = <!BASE_CLASS_FIELD_SHADOWS_DERIVED_CLASS_PROPERTY!>modCount<!>
    }
}

fun main() {
    AbstractMutableList().added()
}

// FILE: Jaba.java
public class Jaba {
    public int modCount = 0;
}
