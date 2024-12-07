
// FILE: test.kt

fun box() {
    B()
    C(1)
    D()
    E(1)
    F()
    G(1)
    J()
    K(1)
    L()
    M()
    N(1)
    O(1)
    O(1, "1")
}

class B()
class C(val a: Int)
class D {
    constructor()
}
class E {
    constructor(i: Int)
}
class F {
    constructor() {
        val a = 1
    }
}
class G {
    constructor(i: Int) {
        val a = 1
    }
}
class J {
    init {
        val a = 1
    }
}
class K(val i: Int) {
    init {
        val a = 1
    }
}
class L {
    constructor() {
        val a = 1
    }

    init {
        val a = 1
    }
}
class M {
    constructor(): this(1) {
        val a = 1
    }

    constructor(i: Int) {
    }
}
class N {
    constructor(i: Int): this() {
        val a = 1
    }

    constructor() {
    }
}
class O<T>(i: T) {
    constructor(i: Int, j: T): this(j) {
    }
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:20 <init>
// test.kt:5 box
// test.kt:6 box
// test.kt:21 <init>
// test.kt:6 box
// test.kt:7 box
// test.kt:23 <init>
// test.kt:7 box
// test.kt:8 box
// test.kt:26 <init>
// test.kt:8 box
// test.kt:9 box
// test.kt:29 <init>
// test.kt:30 <init>
// test.kt:31 <init>
// test.kt:9 box
// test.kt:10 box
// test.kt:34 <init>
// test.kt:35 <init>
// test.kt:36 <init>
// test.kt:10 box
// test.kt:11 box
// test.kt:38 <init>
// test.kt:39 <init>
// test.kt:40 <init>
// test.kt:41 <init>
// test.kt:38 <init>
// test.kt:11 box
// test.kt:12 box
// test.kt:43 <init>
// test.kt:44 <init>
// test.kt:45 <init>
// test.kt:46 <init>
// test.kt:43 <init>
// test.kt:12 box
// test.kt:13 box
// test.kt:49 <init>
// test.kt:53 <init>
// test.kt:54 <init>
// test.kt:55 <init>
// test.kt:50 <init>
// test.kt:51 <init>
// test.kt:13 box
// test.kt:14 box
// test.kt:58 <init>
// test.kt:62 <init>
// test.kt:63 <init>
// test.kt:59 <init>
// test.kt:60 <init>
// test.kt:14 box
// test.kt:15 box
// test.kt:66 <init>
// test.kt:70 <init>
// test.kt:71 <init>
// test.kt:67 <init>
// test.kt:68 <init>
// test.kt:15 box
// test.kt:16 box
// test.kt:73 <init>
// test.kt:16 box
// test.kt:17 box
// test.kt:74 <init>
// test.kt:73 <init>
// test.kt:75 <init>
// test.kt:17 box
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:20 <init>
// test.kt:6 box
// test.kt:21 <init>
// test.kt:21 <init>
// test.kt:7 box
// test.kt:23 D_init_$Init$
// test.kt:22 D
// test.kt:8 box
// test.kt:26 E_init_$Init$
// test.kt:25 E
// test.kt:9 box
// test.kt:29 F_init_$Init$
// test.kt:28 F
// test.kt:30 F_init_$Init$
// test.kt:10 box
// test.kt:34 G_init_$Init$
// test.kt:33 G
// test.kt:35 G_init_$Init$
// test.kt:11 box
// test.kt:40 <init>
// test.kt:38 <init>
// test.kt:12 box
// test.kt:43 <init>
// test.kt:45 <init>
// test.kt:43 <init>
// test.kt:13 box
// test.kt:49 L_init_$Init$
// test.kt:54 L
// test.kt:48 L
// test.kt:50 L_init_$Init$
// test.kt:14 box
// test.kt:58 M_init_$Init$
// test.kt:62 M_init_$Init$
// test.kt:57 M
// test.kt:59 M_init_$Init$
// test.kt:15 box
// test.kt:66 N_init_$Init$
// test.kt:70 N_init_$Init$
// test.kt:65 N
// test.kt:67 N_init_$Init$
// test.kt:16 box
// test.kt:73 <init>
// test.kt:17 box
// test.kt:74 O_init_$Init$
// test.kt:73 <init>
// test.kt:18 box

// EXPECTATIONS WASM
// test.kt:5 $box (4, 4, 4)
// test.kt:20 $B.<init>
// test.kt:6 $box (4, 6, 4, 4)
// test.kt:21 $C.<init> (8, 8, 8, 19)
// test.kt:7 $box (4, 4, 4)
// test.kt:23 $D.<init> (4, 17)
// test.kt:24 $D.<init>
// test.kt:8 $box (4, 6, 4, 4)
// test.kt:26 $E.<init> (4, 23)
// test.kt:27 $E.<init>
// test.kt:9 $box (4, 4, 4)
// test.kt:29 $F.<init>
// test.kt:32 $F.<init>
// test.kt:30 $F.<init> (16, 8)
// test.kt:31 $F.<init>
// test.kt:10 $box (4, 6, 4, 4)
// test.kt:34 $G.<init>
// test.kt:37 $G.<init>
// test.kt:35 $G.<init> (16, 8)
// test.kt:36 $G.<init>
// test.kt:11 $box (4, 4, 4)
// test.kt:40 $J.<init> (16, 8)
// test.kt:42 $J.<init>
// test.kt:12 $box (4, 6, 4, 4)
// test.kt:43 $K.<init> (8, 8, 8, 19)
// test.kt:45 $K.<init> (16, 8)
// test.kt:13 $box (4, 4, 4)
// test.kt:49 $L.<init>
// test.kt:54 $L.<init> (16, 8)
// test.kt:56 $L.<init>
// test.kt:50 $L.<init> (16, 8)
// test.kt:51 $L.<init>
// test.kt:14 $box (4, 4, 4)
// test.kt:58 $M.<init> (24, 19)
// test.kt:62 $M.<init>
// test.kt:64 $M.<init>
// test.kt:63 $M.<init>
// test.kt:59 $M.<init> (16, 8)
// test.kt:60 $M.<init>
// test.kt:15 $box (4, 6, 4, 4)
// test.kt:66 $N.<init>
// test.kt:70 $N.<init>
// test.kt:72 $N.<init>
// test.kt:71 $N.<init>
// test.kt:67 $N.<init> (16, 8)
// test.kt:68 $N.<init>
// test.kt:16 $box (4, 6, 6, 4, 4)
// test.kt:73 $O.<init> (16, 16)
// test.kt:17 $box (4, 6, 9, 9, 9, 9, 4)
// test.kt:74 $O.<init> (36, 31)
// test.kt:75 $O.<init>
// test.kt:18 $box
