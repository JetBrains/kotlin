// FILE: 1.kt
package test

object A { inline fun f() {} }
object B { inline fun g() {} }
object C { inline fun h() {} }

object D {
    inline fun together() {
        A.f()
        C.h()
        B.g()
    }
}

// FILE: 2.kt
import test.*

object X {
    // Unlike `rangeFolding.kt`, the calls in `D.together` refer to different
    // classes which are reflected in the SMAP, so they cannot be joined into
    // a single range even in `X.foo`; neither can lines corresponding to
    // `D.together` because they do not form an uninterrupted range.
    fun foo() = D.together()
}

fun box(): String {
    X.foo()
    return "OK"
}

// FILE: 1.smap
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/D
+ 2 1.kt
test/A
+ 3 1.kt
test/C
+ 4 1.kt
test/B
*L
1#1,16:1
4#2:17
6#3:18
5#4:19
*E
*S KotlinDebug
*F
+ 1 1.kt
test/D
*L
10#1:17
11#1:18
12#1:19
*E

// FILE: 2.smap
SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
X
+ 2 1.kt
test/D
+ 3 1.kt
test/A
+ 4 1.kt
test/C
+ 5 1.kt
test/B
*L
1#1,17:1
10#2:18
11#2:20
12#2:22
13#2:24
4#3:19
6#4:21
5#5:23
*E
*S KotlinDebug
*F
+ 1 2.kt
X
*L
9#1:18
9#1:20
9#1:22
9#1:24
9#1:19
9#1:21
9#1:23
*E
