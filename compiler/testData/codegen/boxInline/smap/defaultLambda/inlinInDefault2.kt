// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
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

inline fun kValue() = "K"

inline fun lParams(initParams: () -> String = {
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
    "O" + kValue()
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
1#1,75:1
71#1,2:76
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
1#1,75:1
30#2:76
*E
*S KotlinDebug
*F
+ 1 1.kt
test/_1Kt$lParams$1
*L
51#1:76
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
test/_1Kt$lParams$1
*L
1#1,10:1
32#2:11
71#2,2:12
30#2:15
51#3:14
69#3:16
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1:11
5#1,2:12
5#1:15
5#1:14
5#1:16
*E