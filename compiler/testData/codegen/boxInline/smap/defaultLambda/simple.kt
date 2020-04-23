
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default


package test
inline fun inlineFun(capturedParam: String, lambda: () -> String = { capturedParam }): String {
    return lambda()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun("OK")
}

// FILE: 1.smap
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$inlineFun$1
*L
1#1,11:1
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
test/_1Kt
+ 3 1.kt
test/_1Kt$inlineFun$1
*L
1#1,9:1
7#2,2:10
7#3:12
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
6#1,2:10
6#1:12
*E
