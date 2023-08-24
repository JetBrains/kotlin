// IGNORE_BACKEND: WASM
// FILE: test.kt

class Foo {
    var a: String

    init {
        a = x()
    }
}

class Bar {
    init {
        val a = 5
    }

    init {
        val b = 2
    }
}

class Boo {
    init {
        val a = 5
    }

    val x = x()

    init {
        val b = 2
    }
}

class Zoo {
    init { val a = 5 }

    init { val b = 6 }

    init {
        val c = 7
    }

    init { val d = 8 }
}

fun x() = ""

fun box() {
    Foo()
    Bar()
    Boo()
    Zoo()
}

// JVM_IR has an extra step back to the line of the class
// declaration for the return in the constructor.

// EXPECTATIONS JVM JVM_IR
// test.kt:49 box
// test.kt:4 <init>
// test.kt:7 <init>
// test.kt:8 <init>
// test.kt:46 x
// test.kt:8 <init>
// test.kt:9 <init>
// EXPECTATIONS JVM_IR
// test.kt:4 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:49 box
// test.kt:50 box
// test.kt:12 <init>
// test.kt:13 <init>
// test.kt:14 <init>
// test.kt:15 <init>
// test.kt:17 <init>
// test.kt:18 <init>
// test.kt:19 <init>
// EXPECTATIONS JVM_IR
// test.kt:12 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:50 box
// test.kt:51 box
// test.kt:22 <init>
// test.kt:23 <init>
// test.kt:24 <init>
// test.kt:25 <init>
// test.kt:27 <init>
// test.kt:46 x
// test.kt:27 <init>
// test.kt:29 <init>
// test.kt:30 <init>
// test.kt:31 <init>
// EXPECTATIONS JVM_IR
// test.kt:22 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:51 box
// test.kt:52 box
// test.kt:34 <init>
// test.kt:35 <init>
// test.kt:37 <init>
// test.kt:39 <init>
// test.kt:40 <init>
// test.kt:41 <init>
// test.kt:43 <init>
// EXPECTATIONS JVM_IR
// test.kt:34 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:52 box
// test.kt:53 box

// EXPECTATIONS JS_IR
// test.kt:49 box
// test.kt:8 <init>
// test.kt:46 x
// test.kt:4 <init>
// test.kt:50 box
// test.kt:14 <init>
// test.kt:18 <init>
// test.kt:12 <init>
// test.kt:51 box
// test.kt:24 <init>
// test.kt:27 <init>
// test.kt:46 x
// test.kt:30 <init>
// test.kt:22 <init>
// test.kt:52 box
// test.kt:35 <init>
// test.kt:37 <init>
// test.kt:40 <init>
// test.kt:43 <init>
// test.kt:34 <init>
// test.kt:53 box