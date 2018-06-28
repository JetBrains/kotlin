// FILE: 1.kt

class A {
    inline fun foo() {}
}

// FILE: 2.kt

fun box(): String {
    A().foo()

    return "OK"
}

// FILE: 2.smap

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 1.kt
A
*L
1#1,9:1
4#2:10
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
4#1:10
*E