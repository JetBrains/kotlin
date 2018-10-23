// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

inline fun (() -> String).test(): (() -> String) = { invoke() + this.invoke() + this() }

// call this.hashCode() guarantees that extension receiver is noinline by default
inline fun (() -> String).extensionNoInline(): String = this() + (this.hashCode().toString())

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING

fun box(): String {
    val res = { "OK" }.test()()
    if (res != "OKOKOK") return "fail 1: $res"

    val res2 = { "OK" }.extensionNoInline().subSequence(0, 2)
    if (res2 != "OK") return "fail 2: $res2"

    return "OK"
}
