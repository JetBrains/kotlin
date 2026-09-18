// FILE: test.kt
var variant = 0

fun foo() {
    bar {
        try {
            baz()
        }
        catch (e: RuntimeException) {
            e.toString()
        }
    }

    bar {
        when (boo()) {
            "boo" -> baz()
            else -> "111"
        }
    }
}

inline fun bar(x: () -> String): String {
    return x()
}

fun baz() = if (variant == 0) "baz" else throw RuntimeException()

fun boo() = if (variant == 1) "boo" else "nope"

fun box() {
    foo()
    variant += 1
    foo()
}

// EXPECTATIONS JVM_IR
// test.kt:31 box
// test.kt:5 foo
// test.kt:23 foo
// test.kt:6 foo
// test.kt:7 foo
// test.kt:26 baz
// test.kt:7 foo
// test.kt:11 foo
// test.kt:23 foo
// test.kt:14 foo
// test.kt:23 foo
// test.kt:15 foo
// test.kt:28 boo
// test.kt:16 foo
// test.kt:17 foo
// test.kt:18 foo
// test.kt:23 foo
// test.kt:20 foo
// test.kt:32 box
// test.kt:33 box
// test.kt:5 foo
// test.kt:23 foo
// test.kt:6 foo
// test.kt:7 foo
// test.kt:26 baz
// test.kt:9 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:23 foo
// test.kt:14 foo
// test.kt:23 foo
// test.kt:15 foo
// test.kt:28 boo
// test.kt:16 foo
// test.kt:26 baz

// EXPECTATIONS JS_IR
// test.kt:31 box
// test.kt:7 foo
// test.kt:26 baz
// test.kt:26 baz
// test.kt:26 baz
// test.kt:15 foo
// test.kt:28 boo
// test.kt:20 foo
// test.kt:32 box
// test.kt:33 box
// test.kt:7 foo
// test.kt:26 baz
// test.kt:26 baz
// test.kt:10 foo
// test.kt:15 foo
// test.kt:28 boo
// test.kt:16 foo
// test.kt:26 baz
// test.kt:26 baz

// EXPECTATIONS WASM
// test.kt:31 $box (4)
// test.kt:5 $foo (4)
// test.kt:23 $foo (11)
// test.kt:7 $foo (12)
// test.kt:26 $baz (16, 27, 16, 30, 65)
// test.kt:11 $foo (9)
// test.kt:23 $foo (4)
// test.kt:14 $foo (4)
// test.kt:23 $foo (11)
// test.kt:15 $foo (14)
// test.kt:28 $boo (16, 27, 16, 41, 47)
// test.kt:16 $foo (12)
// test.kt:17 $foo (20)
// test.kt:18 $foo (9)
// test.kt:23 $foo (4)
// test.kt:20 $foo (1)
// test.kt:32 $box (4, 15, 4)
// test.kt:33 $box (4)
// test.kt:5 $foo (4)
// test.kt:23 $foo (11)
// test.kt:7 $foo (12)
// test.kt:26 $baz (16, 27, 16, 47, 41)
// test.kt:10 $foo (12, 14)
// test.kt:11 $foo (9)
// test.kt:23 $foo (4)
// test.kt:14 $foo (4)
// test.kt:23 $foo (11)
// test.kt:15 $foo (14)
// test.kt:28 $boo (16, 27, 16, 30, 47)
// test.kt:16 $foo (12, 21)
// test.kt:26 $baz (16, 27, 16, 47, 41)

// EXPECTATIONS NATIVE
// test.kt:31 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:23 foo
// test.kt:7 foo
// test.kt:26 baz
// test.kt:2 <get-variant>
// test.kt:26 baz
// test.kt:7 foo
// test.kt:11 foo
// test.kt:23 foo
// test.kt:14 foo
// test.kt:23 foo
// test.kt:15 foo
// test.kt:28 boo
// test.kt:2 <get-variant>
// test.kt:28 boo
// test.kt:15 foo
// test.kt:16 foo
// test.kt:17 foo
// test.kt:18 foo
// test.kt:23 foo
// test.kt:20 foo
// test.kt:32 box
// test.kt:2 <get-variant>
// test.kt:32 box
// test.kt:2 <set-variant>
// test.kt:33 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:23 foo
// test.kt:7 foo
// test.kt:26 baz
// test.kt:2 <get-variant>
// test.kt:26 baz
// test.kt:26 baz
