//FILE: 1.kt
// IGNORE_BACKEND: JVM_IR
package test
inline fun annotatedWith2(crossinline predicate: () -> Boolean) =
        { any { predicate() } }()


inline fun annotatedWith(crossinline predicate: () -> Boolean) =
        annotatedWith2 { predicate() }


inline fun any(s: () -> Boolean) {
    s()
}


//FILE: 2.kt
import test.*

fun box(): String {
    var result = "fail"

    annotatedWith { result = "OK"; true }

    return result
}


inline fun test(z: () -> Unit) {
    z()
}


// FILE: 2.smap
//*L
//1#1,15:1
//17#1:19


SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 1.kt
test/_1Kt
*L
1#1,18:1
9#2:19
5#2:20
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
7#1:19
7#1:20
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$annotatedWith2$1
+ 2 1.kt
test/_1Kt
+ 3 2.kt
_2Kt
*L
1#1,17:1
13#2,2:18
9#2:20
7#3:21
*E