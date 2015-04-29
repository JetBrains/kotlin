// FILE: k.kt

trait K {
    fun <T> foo(t: T)
}

// FILE: J.java

interface J extends K {
    <T> void foo(T t);
}