// !DIAGNOSTICS: -UNUSED_PARAMETER
open class Outer {
    inner open class A protected constructor(x: Int) {
        protected constructor() : this(1)

        protected constructor(x: String) : this(2)
    }

    inner class B1 : A(1) {}
    inner class B2 : A() {}
    inner class B3 : A("") {}

    inner class B4 : A {
        constructor() : super(1)
        constructor(x: Int) : super()
        constructor(x: Int, y: Int) : super("")
    }
}

