package smartStepIntoConstructor

fun main(args: Array<String>) {
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    B()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    C(1)
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    D()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    E(1)
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    F()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    G(1)
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    J()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    K(1)
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    L()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    M()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    N(1)
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    O(1)
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
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