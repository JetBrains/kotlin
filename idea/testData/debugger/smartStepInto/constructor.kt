fun foo() {
    println("${A()} ${B()} ${C(1)} ${D()} ${E(1)} ${F()} ${G()} ${J()} ${K(1)} ${L()}")<caret>
}

class A
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

    }
}
class G {
    constructor(i: Int) {

    }
}
class J {
    init {

    }
}
class K(val a: Int) {
    init {

    }
}
class L {
    constructor() {

    }

    init {

    }
}

// EXISTS: println(Any?), constructor B(), constructor C(Int), constructor D(), constructor E(Int), constructor F(), constructor G(Int), constructor J(), constructor K(Int), constructor L()