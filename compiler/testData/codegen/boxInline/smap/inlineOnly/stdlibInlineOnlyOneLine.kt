// FILE: 1.kt
package test

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun <T, R> T.myLet(block: (T) -> R) = block(this)

// FILE: 2.kt
import test.*

fun box(): String {
    val k = "".myLet { it + "K" }
    return "O".myLet(fun (it: String): String { return it + k })
}

// FILE: 2.smap
// See KT-23064 for the problem and InlineOnlySmapSkipper for an explanation.
SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 fake.kt
kotlin/jvm/internal/FakeKt
*L
1#1,9:1
65100#2:10
*E
