// WITH_STDLIB
// IGNORE_BACKEND: JS_IR JS_IR_ES6 NATIVE
// ^ KT-61141: absent enum fake_overrides: finalize (K1), getDeclaringClass (K1), clone (K2)

enum class Test0(val x: Int) {
    ZERO;
    constructor() : this(0)
}

enum class Test1(val x: Int) {
    ZERO, ONE(1);
    constructor() : this(0)
}

enum class Test2(val x: Int) {
    ZERO {
        override fun foo() {
            println("ZERO")
        }
    },
    ONE(1) {
        override fun foo() {
            println("ONE")
        }
    };
    constructor() : this(0)
    abstract fun foo()
}
