// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57775

// KT-61141: absent enum fake_overrides: finalize(K1), getDeclaringClass(K1), clone(K2),
// IGNORE_BACKEND: NATIVE

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
