// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: 1.kt

@file:JvmName("Facade")
@file:JvmMultifileClass

inline fun foo(l: () -> String): String = l()

inline fun foo2(l: () -> String): String = foo(l)


// FILE: 2.kt

fun box(): String = foo { "OK" }

// FILE: 1.smap
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
Facade___1Kt
*L
1#1,13:1
8#1:14
*E
*S KotlinDebug
*F
+ 1 1.kt
Facade___1Kt
*L
10#1:14
*E

// FILE: 2.smap
SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 1.kt
Facade___1Kt
*L
1#1,5:1
8#2:6
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
3#1:6
*E