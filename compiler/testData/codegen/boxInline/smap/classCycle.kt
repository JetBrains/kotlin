// FILE: 1.kt
package test

class A {
    inline fun a() = B().b()
    inline fun c() = B().d()
}

class B {
    inline fun b() = A().c()
    inline fun d() = "OK"
}

// FILE: 2.kt
import test.*

fun box() = A().a()

// FILE: 1.smap
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/B
+ 2 1.kt
test/A
*L
1#1,14:1
11#1:16
6#2:15
*E
*S KotlinDebug
*F
+ 1 1.kt
test/B
*L
10#1:16
10#1:15
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/A
+ 2 1.kt
test/B
*L
1#1,14:1
6#1:16
10#2:15
11#2:17
11#2:18
*E
*S KotlinDebug
*F
+ 1 1.kt
test/A
*L
5#1:16
5#1:15
5#1:17
6#1:18
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
test/A
+ 3 1.kt
test/B
*L
1#1,6:1
5#2:7
6#2:9
10#3:8
11#3:10
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
4#1:7
4#1:9
4#1:8
4#1:10
*E
