// WITH_STDLIB

// FILE: test.kt
object O {
    init {
        println()
    }
}

open class A(x: Int)

class B :
        A(
                23
        )

class C : A {
    constructor(x: Int) :
        super(
                x
        ) {
        println()
    }

    constructor() :
            this (
                    42
            )
}

fun box() {
    val o = O
    val b = B()
    val c1 = C(0)
    val c2 = C()
}

// EXPECTATIONS JVM_IR
// test.kt:32 box
// test.kt:5 <clinit>
// test.kt:6 <clinit>
// test.kt:7 <clinit>
// test.kt:33 box
// test.kt:12 <init>
// test.kt:14 <init>
// test.kt:12 <init>
// test.kt:10 <init>
// test.kt:12 <init>
// test.kt:33 box
// test.kt:34 box
// test.kt:19 <init>
// test.kt:20 <init>
// test.kt:19 <init>
// test.kt:10 <init>
// test.kt:22 <init>
// test.kt:23 <init>
// test.kt:34 box
// test.kt:35 box
// test.kt:26 <init>
// test.kt:27 <init>
// test.kt:26 <init>
// test.kt:19 <init>
// test.kt:20 <init>
// test.kt:19 <init>
// test.kt:10 <init>
// test.kt:22 <init>
// test.kt:23 <init>
// test.kt:28 <init>
// test.kt:35 box
// test.kt:36 box

// EXPECTATIONS JS_IR
// test.kt:32 box
// test.kt:6 <init>
// test.kt:4 <init>
// test.kt:33 box
// test.kt:12 <init>
// test.kt:10 <init>
// test.kt:12 <init>
// test.kt:34 box
// test.kt:19 C_init_$Init$
// test.kt:10 <init>
// test.kt:18 C_init_$Init$
// test.kt:22 C_init_$Init$
// test.kt:35 box
// test.kt:26 C_init_$Init$
// test.kt:19 C_init_$Init$
// test.kt:10 <init>
// test.kt:18 C_init_$Init$
// test.kt:22 C_init_$Init$
// test.kt:36 box

// EXPECTATIONS WASM
// test.kt:33 $box (12)
// test.kt:6 $O.<init> (8)
// test.kt:8 $O.<init> (1)
// test.kt:33 $box (12)
// test.kt:14 $B.<init> (16)
// test.kt:12 $B.<init> (0)
// test.kt:10 $A.<init> (20)
// test.kt:12 $B.<init> (0)
// test.kt:15 $B.<init> (9)
// test.kt:33 $box (12)
// test.kt:34 $box (13, 15, 13)
// test.kt:20 $C.<init> (16)
// test.kt:19 $C.<init> (8)
// test.kt:10 $A.<init> (20)
// test.kt:19 $C.<init> (8)
// test.kt:18 $C.<init> (4)
// test.kt:22 $C.<init> (8)
// test.kt:23 $C.<init> (5)
// test.kt:34 $box (13)
// test.kt:35 $box (13)
// test.kt:27 $C.<init> (20)
// test.kt:26 $C.<init> (12)
// test.kt:20 $C.<init> (16)
// test.kt:19 $C.<init> (8)
// test.kt:10 $A.<init> (20)
// test.kt:19 $C.<init> (8)
// test.kt:18 $C.<init> (4)
// test.kt:22 $C.<init> (8)
// test.kt:23 $C.<init> (5)
// test.kt:26 $C.<init> (12)
// test.kt:28 $C.<init> (13)
// test.kt:35 $box (13)
// test.kt:36 $box (1)

// EXPECTATIONS NATIVE
// test.kt:32 box
// test.kt:4 <get-$instance>
// test.kt:1 <get-$instance>
// test.kt:8 <get-$instance>
// test.kt:32 box
// test.kt:33 box
// test.kt:12 <init>
// test.kt:10 <init>
// test.kt:15 <init>
// test.kt:33 box
// test.kt:34 box
// test.kt:18 <init>
// test.kt:19 <init>
// test.kt:20 <init>
// test.kt:19 <init>
// test.kt:10 <init>
// test.kt:22 <init>
// test.kt:23 <init>
// test.kt:34 box
// test.kt:35 box
// test.kt:25 <init>
// test.kt:26 <init>
// test.kt:18 <init>
// test.kt:19 <init>
// test.kt:20 <init>
// test.kt:19 <init>
// test.kt:10 <init>
// test.kt:22 <init>
// test.kt:23 <init>
// test.kt:28 <init>
// test.kt:35 box
// test.kt:36 box
