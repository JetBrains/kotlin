// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// FILE: 1.kt

package test
inline fun inlineFun(capturedParam: String, noinline lambda: () -> String = { capturedParam }): String {
    return lambda()
}

// FILE: 2.kt
//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    return inlineFun("OK")
}

// FILE: 1.smap
//TODO maybe do smth with default method body mapping
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt
*L
1#1,9:1
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$inlineFun$1
*L
1#1,9:1
*E

// FILE: 2.TODO