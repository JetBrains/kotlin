// FILE: TypeParamInInner2.java
package test;

class TypeParamInInner2 {
    void check() {
       TypeParamInInner2Kt.f("OK");
    }
}

// FILE: TypeParamInInner2.kt
package test

fun <V> f(x: V): Int {
    fun g(y: V) = 2
    return g(x)
}
