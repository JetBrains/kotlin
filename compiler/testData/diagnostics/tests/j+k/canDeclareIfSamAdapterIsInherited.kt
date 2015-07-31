// FILE: Super.java
class Super {
    void foo(Runnable r) {
    }
}

// FILE: Sub.kt
class Sub() : Super() {
    fun foo(<!UNUSED_PARAMETER!>r<!> : (() -> Unit)?) {
    }
}