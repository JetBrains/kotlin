// IGNORE_BACKEND: WASM
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

// JVM_IR consistently steps through constructor start line, constructor body constructor end line.
// JVM does not. The JVM behavior is unfortunate for instance for the L class above. Stepping through
// construction on the JVM will give the sequence 49, 52, 53, 49 which makes it unclear if the assignment
// on line 49 was carried out before or after the assignment in the init block. The JVM_IR sequence is
// 48, 52, 53, 54, 49, 50 which makes the sequence clear.

// In addition JVM_IR consistently steps on the init line and on the init end brace. The line numbers
// are there in the class file fro JVM, but there is no guarantee that there is an instruction to
// step on and sometimes there is no step on the end brace.

// EXPECTATIONS JVM JVM_IR
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
// EXPECTATIONS JVM_IR
// test.kt:29 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:30 <init>
// EXPECTATIONS JVM_IR
// test.kt:31 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:9 box
// test.kt:10 box
// EXPECTATIONS JVM_IR
// test.kt:34 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:35 <init>
// EXPECTATIONS JVM_IR
// test.kt:36 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:10 box
// test.kt:11 box
// test.kt:38 <init>
// test.kt:39 <init>
// test.kt:40 <init>
// test.kt:41 <init>
// EXPECTATIONS JVM_IR
// test.kt:38 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:11 box
// test.kt:12 box
// test.kt:43 <init>
// test.kt:44 <init>
// test.kt:45 <init>
// test.kt:46 <init>
// EXPECTATIONS JVM_IR
// test.kt:43 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:12 box
// test.kt:13 box
// EXPECTATIONS JVM
// test.kt:50 <init>
// EXPECTATIONS JVM_IR
// test.kt:49 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:53 <init>
// test.kt:54 <init>
// EXPECTATIONS JVM_IR
// test.kt:55 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:50 <init>
// EXPECTATIONS JVM_IR
// test.kt:51 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:13 box
// test.kt:14 box
// test.kt:58 <init>
// test.kt:62 <init>
// EXPECTATIONS JVM_IR
// test.kt:63 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:59 <init>
// EXPECTATIONS JVM_IR
// test.kt:60 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:14 box
// test.kt:15 box
// test.kt:66 <init>
// test.kt:70 <init>
// EXPECTATIONS JVM_IR
// test.kt:71 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:67 <init>
// EXPECTATIONS JVM_IR
// test.kt:68 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:15 box
// test.kt:16 box
// test.kt:73 <init>
// test.kt:16 box
// test.kt:17 box
// test.kt:74 <init>
// test.kt:73 <init>
// EXPECTATIONS JVM
// test.kt:74 <init>
// EXPECTATIONS JVM_IR
// test.kt:75 <init>
// EXPECTATIONS JVM JVM_IR
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
