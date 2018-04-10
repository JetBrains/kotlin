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
    return run {
        lParams()
    }
}

// FILE: 1.smap
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt
*L
1#1,38:1
34#1,2:39
*E

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
1#1,38:1
29#2:39
*E
*S KotlinDebug
*F
+ 1 1.kt
test/_1Kt$lParams$1
*L
32#1:39
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
1#1,38:1
32#2:39
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$kValue$1
*L
1#1,38:1
*E

// FILE: 2.smap-nonseparate-compilation

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 Standard.kt
kotlin/StandardKt__StandardKt
+ 3 1.kt
test/_1Kt
+ 4 1.kt
test/_1Kt$lParams$1
*L
1#1,10:1
34#2,4:11
31#3,5:15
29#3:21
32#4:20
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1,4:11
5#1,5:15
5#1:21
5#1:20
*E

// FILE: 2.smap-separate-compilation
SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 Standard.kt
kotlin/StandardKt__StandardKt
+ 3 1.kt
test/_1Kt
+ 4 1.kt
test/_1Kt$lParams$1
*L
1#1,10:1
34#2,4:11
31#3,5:15
29#3:21
32#4:20
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1,4:11
5#1,5:15
5#1:21
5#1:20
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
1#1,38:1
32#2:39
*E