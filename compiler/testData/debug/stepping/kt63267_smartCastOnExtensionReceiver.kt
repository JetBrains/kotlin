// FILE: test.kt

class C {
    fun Any?.f() {
        if (this == null) return

        m()
        m()
        m()
        m()
        m()
    }

    fun Any.m() {}
}

fun box() {
    with(C()) {
        Any().f()
    }
}

// EXPECTATIONS JVM_IR
// test.kt:18 box
// test.kt:3 <init>
// test.kt:18 box
// test.kt:19 box
// test.kt:5 f
// test.kt:7 f
// test.kt:14 m
// test.kt:8 f
// test.kt:14 m
// test.kt:9 f
// test.kt:14 m
// test.kt:10 f
// test.kt:14 m
// test.kt:11 f
// test.kt:14 m
// test.kt:12 f
// test.kt:20 box
// test.kt:18 box
// test.kt:21 box

// EXPECTATIONS JS_IR
// test.kt:19 box
// test.kt:3 <init>
// test.kt:19 box
// test.kt:5 f
// test.kt:7 f
// test.kt:14 m
// test.kt:8 f
// test.kt:14 m
// test.kt:9 f
// test.kt:14 m
// test.kt:10 f
// test.kt:14 m
// test.kt:11 f
// test.kt:14 m
// test.kt:12 f
// test.kt:21 box

// EXPECTATIONS WASM FIR
// test.kt:18 $box (9)
// test.kt:15 $C.<init> (1)
// test.kt:18 $box (9, 4)
// test.kt:19 $box (8, 14)
// test.kt:5 $C.f (12)
// test.kt:7 $C.f (8)
// test.kt:14 $C.m (18)
// test.kt:8 $C.f (8)
// test.kt:14 $C.m (18)
// test.kt:9 $C.f (8)
// test.kt:14 $C.m (18)
// test.kt:10 $C.f (8)
// test.kt:14 $C.m (18)
// test.kt:11 $C.f (8)
// test.kt:14 $C.m (18)
// test.kt:12 $C.f (5)
// test.kt:19 $box (14)
// test.kt:20 $box (5)
// test.kt:21 $box (1)
