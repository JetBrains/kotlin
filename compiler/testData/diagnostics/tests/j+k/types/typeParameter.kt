// FILE: k.kt

interface K {
    fun <T> foo(t: T)
}

// FILE: J.java

interface J extends K {
    <T> void foo(T t);
}