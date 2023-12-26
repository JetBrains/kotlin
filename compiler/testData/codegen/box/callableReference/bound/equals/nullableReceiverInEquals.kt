// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// JVM_ABI_K1_K2_DIFF: KT-63859

// See https://youtrack.jetbrains.com/issue/KT-14938
// WITH_REFLECT

class A

val a = A()
val aa = A()

fun A?.foo() {}

var A?.bar: Int
    get() = 42
    set(value) {}

val aFoo = a::foo
val A_foo = A::foo
val nullFoo = null::foo

val aBar = a::bar
val A_bar = A::bar
val nullBar = null::bar

fun box(): String =
        when {
            nullFoo != null::foo -> "Bound extension refs with same receiver SHOULD be equal"
            nullFoo == aFoo -> "Bound extension refs with different receivers SHOULD NOT be equal"
            nullFoo == A_foo -> "Bound extension ref with receiver 'null' SHOULD NOT be equal to free ref"

            nullBar != null::bar -> "Bound extension property refs with same receiver SHOULD be equal"
            nullBar == aBar -> "Bound extension property refs with different receivers SHOULD NOT be equal"
            nullBar == A_bar -> "Bound extension property ref with receiver 'null' SHOULD NOT be equal to free property ref"

            else -> "OK"
        }
