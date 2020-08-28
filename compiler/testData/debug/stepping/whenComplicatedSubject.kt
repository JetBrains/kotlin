// FILE: test.kt

fun foo(x: Int) {
   when (val y =
           when {
             x == 0 -> 1
             x == 1 -> 2
             else -> 0
         }) {
       0 -> 3
       1 -> 4
   }
}

fun box() {
    foo(0)
    foo(1)
    foo(2)
}

// The JVM_IR backend optimizes the inner when to a switch and therefore goes directly to the
// right case without stepping through evaluation of each condition.

// LINENUMBERS
// test.kt:16 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:4 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:17 box
// test.kt:5 foo
// LINENUMBERS JVM
// test.kt:6 foo
// LINENUMBERS
// test.kt:7 foo
// test.kt:4 foo
// test.kt:13 foo
// test.kt:18 box
// test.kt:5 foo
// LINENUMBERS JVM
// test.kt:6 foo
// test.kt:7 foo
// LINENUMBERS
// test.kt:8 foo
// test.kt:4 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:19 box