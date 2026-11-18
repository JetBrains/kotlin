// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt
// KT-91568: only `this` itself is renamed to `this_`, not every local whose name starts with "this".

class A {
    inline fun size(list: List<Int>): Int {
        val thisList = list
        val thisSize = thisList.size
        return thisSize + hashCode() * 0
    }
}

fun box() {
    val a = A()
    a.size(listOf(1, 2))
}

// EXPECTATIONS JVM +USE_INLINE_SCOPES_NUMBERS
// test.kt:15 box:
// test.kt:6 <init>:
// test.kt:15 box:
// test.kt:16 box: a:A=A
// test.kt:8 box: a:A=A, this_\1:A=A, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$size\1\16:int=0:int
// test.kt:9 box: a:A=A, this_\1:A=A, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$size\1\16:int=0:int, thisList\1:java.util.List=java.util.Arrays$ArrayList
// test.kt:10 box: a:A=A, this_\1:A=A, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$size\1\16:int=0:int, thisList\1:java.util.List=java.util.Arrays$ArrayList, thisSize\1:int=2:int
// test.kt:17 box: a:A=A

// EXPECTATIONS JVM
// test.kt:15 box:
// test.kt:6 <init>:
// test.kt:15 box:
// test.kt:16 box: a:A=A
// test.kt:8 box: a:A=A, this_$iv:A=A, list$iv:java.util.List=java.util.Arrays$ArrayList, $i$f$size:int=0:int
// test.kt:9 box: a:A=A, this_$iv:A=A, list$iv:java.util.List=java.util.Arrays$ArrayList, $i$f$size:int=0:int, thisList$iv:java.util.List=java.util.Arrays$ArrayList
// test.kt:10 box: a:A=A, this_$iv:A=A, list$iv:java.util.List=java.util.Arrays$ArrayList, $i$f$size:int=0:int, thisList$iv:java.util.List=java.util.Arrays$ArrayList, thisSize$iv:int=2:int
// test.kt:17 box: a:A=A
