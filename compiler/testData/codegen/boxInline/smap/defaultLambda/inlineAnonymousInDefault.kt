// IGNORE_BACKEND: JS

// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: lParams$default

package test
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]
//A lot of blank lines [Don't delete]

inline fun kValue(crossinline s: () -> String) = { s() + "K" }()

inline fun lParams(initParams: () -> String = {
    "" + kValue { "O" }
}): String {
    val z = "body"
    return initParams()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return lParams()
}

// FILE: 1.smap
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$lParams$1
+ 2 1.kt
test/_1Kt
*L
1#1,40:1
31#2:41
*E
*S KotlinDebug
*F
+ 1 1.kt
test/_1Kt$lParams$1
*L
34#1:41
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$kValue$1
+ 2 1.kt
test/_1Kt$lParams$1
*L
1#1,40:1
34#2:41
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$kValue$1
*L
1#1,40:1
*E

// FILE: 2.smap-nonseparate-compilation

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
test/_1Kt$lParams$1
*L
1#1,8:1
33#2,5:9
31#2:15
34#3:14
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1,5:9
5#1:15
5#1:14
*E

// FILE: 2.smap-separate-compilation

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
test/_1Kt$lParams$1
*L
1#1,8:1
33#2,5:9
31#2:15
34#3:14
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1,5:9
5#1:15
5#1:14
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$kValue$1
+ 2 1.kt
test/_1Kt$lParams$1
*L
1#1,40:1
34#2:41
*E