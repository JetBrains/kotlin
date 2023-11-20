// IGNORE_BACKEND: JS_IR JS_IR_ES6 NATIVE
// ^ KT-61141: absent enum fake_overrides: finalize (K1), getDeclaringClass (K1), clone (K2)

enum class X {

    B {
        val value2 = "OK"
        override val value = { value2 }
    };

    abstract val value: () -> String
}

fun box(): String {
    return X.B.value()
}
