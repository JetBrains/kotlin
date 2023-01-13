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
// test.kt:4 box
// test.kt:19 <init>
// test.kt:4 box
// test.kt:5 box
// test.kt:20 <init>
// test.kt:5 box
// test.kt:6 box
// test.kt:22 <init>
// test.kt:6 box
// test.kt:7 box
// test.kt:25 <init>
// test.kt:7 box
// test.kt:8 box
// EXPECTATIONS JVM_IR
// test.kt:28 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:29 <init>
// EXPECTATIONS JVM_IR
// test.kt:30 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:8 box
// test.kt:9 box
// EXPECTATIONS JVM_IR
// test.kt:33 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:34 <init>
// EXPECTATIONS JVM_IR
// test.kt:35 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:9 box
// test.kt:10 box
// test.kt:37 <init>
// test.kt:38 <init>
// test.kt:39 <init>
// test.kt:40 <init>
// EXPECTATIONS JVM_IR
// test.kt:37 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:10 box
// test.kt:11 box
// test.kt:42 <init>
// test.kt:43 <init>
// test.kt:44 <init>
// test.kt:45 <init>
// EXPECTATIONS JVM_IR
// test.kt:42 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:11 box
// test.kt:12 box
// EXPECTATIONS JVM
// test.kt:49 <init>
// EXPECTATIONS JVM_IR
// test.kt:48 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:52 <init>
// test.kt:53 <init>
// EXPECTATIONS JVM_IR
// test.kt:54 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:49 <init>
// EXPECTATIONS JVM_IR
// test.kt:50 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:12 box
// test.kt:13 box
// test.kt:57 <init>
// test.kt:61 <init>
// EXPECTATIONS JVM_IR
// test.kt:62 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:58 <init>
// EXPECTATIONS JVM_IR
// test.kt:59 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:13 box
// test.kt:14 box
// test.kt:65 <init>
// test.kt:69 <init>
// EXPECTATIONS JVM_IR
// test.kt:70 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:66 <init>
// EXPECTATIONS JVM_IR
// test.kt:67 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:14 box
// test.kt:15 box
// test.kt:72 <init>
// test.kt:15 box
// test.kt:16 box
// test.kt:73 <init>
// test.kt:72 <init>
// EXPECTATIONS JVM
// test.kt:73 <init>
// EXPECTATIONS JVM_IR
// test.kt:74 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:16 box
// test.kt:17 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:19 <init>
// test.kt:5 box
// test.kt:20 <init>
// test.kt:20 <init>
// test.kt:6 box
// test.kt:22 D_init_$Init$
// test.kt:21 D
// test.kt:7 box
// test.kt:25 E_init_$Init$
// test.kt:24 E
// test.kt:8 box
// test.kt:28 F_init_$Init$
// test.kt:27 F
// test.kt:29 F_init_$Init$
// test.kt:9 box
// test.kt:33 G_init_$Init$
// test.kt:32 G
// test.kt:34 G_init_$Init$
// test.kt:10 box
// test.kt:39 <init>
// test.kt:37 <init>
// test.kt:11 box
// test.kt:42 <init>
// test.kt:44 <init>
// test.kt:42 <init>
// test.kt:12 box
// test.kt:48 L_init_$Init$
// test.kt:53 L
// test.kt:47 L
// test.kt:49 L_init_$Init$
// test.kt:13 box
// test.kt:57 M_init_$Init$
// test.kt:61 M_init_$Init$
// test.kt:56 M
// test.kt:58 M_init_$Init$
// test.kt:14 box
// test.kt:65 N_init_$Init$
// test.kt:69 N_init_$Init$
// test.kt:64 N
// test.kt:66 N_init_$Init$
// test.kt:15 box
// test.kt:72 <init>
// test.kt:16 box
// test.kt:73 O_init_$Init$
// test.kt:72 <init>
// test.kt:17 box
