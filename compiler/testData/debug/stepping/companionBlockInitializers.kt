// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: test.kt

// Single companion block: several initializers, including a multi-statement one.
class Single {
    companion {
        val a = compute()

        val b = run {
            val x = 32
            x.toString()
        }
    }
}

// Multiple companion blocks: initialization follows program order (KEEP §3.2).
class Multiple {
    companion {
        val first = compute()
    }

    companion {
        val second = run {
            val y = 42
            y.toString()
        }
    }
}

// Inheritance: accessing a child companion member triggers the parent companion
// initializer before the child's (KEEP §3.3).
open class Parent {
    companion {
        val p = compute()
    }
}

class Child : Parent() {
    companion {
        val c = compute()
    }
}

fun compute() = ""

fun box() {
    Single.a
    Single.b
    Multiple.first
    Multiple.second
    Child.c
}

// EXPECTATIONS JVM_IR
// test.kt:47 box
// test.kt:7 <clinit>
// test.kt:9 <clinit>
// test.kt:10 <clinit>
// test.kt:11 <clinit>
// test.kt:9 <clinit>
// test.kt:9 <clinit>
// test.kt:7 getA
// test.kt:47 box
// test.kt:48 box
// test.kt:9 getB
// test.kt:48 box
// test.kt:49 box
// test.kt:19 <clinit>
// test.kt:23 <clinit>
// test.kt:24 <clinit>
// test.kt:25 <clinit>
// test.kt:23 <clinit>
// test.kt:23 <clinit>
// test.kt:19 getFirst
// test.kt:49 box
// test.kt:50 box
// test.kt:23 getSecond
// test.kt:50 box
// test.kt:51 box
// test.kt:34 <clinit>
// test.kt:40 <clinit>
// test.kt:40 getC
// test.kt:51 box
// test.kt:52 box

// EXPECTATIONS JS_IR
// test.kt:47 box
// test.kt:7 static_init_5
// test.kt:44 compute
// test.kt:10 static_init_5
// test.kt:11 static_init_5
// test.kt:48 box
// test.kt:49 box
// test.kt:19 static_init_6
// test.kt:44 compute
// test.kt:24 static_init_6
// test.kt:25 static_init_6
// test.kt:50 box
// test.kt:51 box
// test.kt:34 static_init_7
// test.kt:44 compute
// test.kt:40 static_init_8
// test.kt:44 compute
// test.kt:52 box

// EXPECTATIONS WASM
// test.kt:47 $box (11)
// test.kt:7 $static_init (16)
// test.kt:44 $compute (16, 18)
// test.kt:7 $static_init (8)
// test.kt:9 $static_init (16)
// test.kt:10 $static_init (20)
// test.kt:11 $static_init (12, 14, 24)
// test.kt:9 $static_init (8)
// test.kt:47 $box (11)
// test.kt:48 $box (11)
// test.kt:7 $static_init (16)
// test.kt:48 $box (11)
// test.kt:49 $box (13)
// test.kt:19 $static_init (20)
// test.kt:44 $compute (16, 18)
// test.kt:19 $static_init (8)
// test.kt:23 $static_init (21)
// test.kt:24 $static_init (20)
// test.kt:25 $static_init (12, 14, 24)
// test.kt:23 $static_init (8)
// test.kt:49 $box (13)
// test.kt:50 $box (13)
// test.kt:19 $static_init (20)
// test.kt:50 $box (13)
// test.kt:51 $box (10)
// test.kt:40 $static_init (16)
// test.kt:34 $static_init (16)
// test.kt:44 $compute (16, 18)
// test.kt:34 $static_init (8)
// test.kt:40 $static_init (16)
// test.kt:44 $compute (16, 18)
// test.kt:40 $static_init (8)
// test.kt:51 $box (10)
// test.kt:52 $box (1)
