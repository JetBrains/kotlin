// ISSUE: KT-77103
// WITH_STDLIB
// LANGUAGE: -IrInlinerBeforeKlibSerialization
// NO_CHECK_LAMBDA_INLINING

// SKIP_UNBOUND_IR_SERIALIZATION
// ^^^ KT-76998: Cannot deserialize inline fun: FUN name:foo visibility:public modality:FINAL returnType:kotlin.Unit [inline]
//   VALUE_PARAMETER kind:Regular name:block index:0 type:kotlin.Function0<kotlin.Unit>

// FILE: 1.kt
inline fun foo(block: () -> Unit) {}

fun app() {
    foo {
        fun bar() {
            val localDelegatedProperty by lazy { false }
        }
    }
}

// FILE: 2.kt
fun box(): String {
    app()
    return "OK"
}
