// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY

inline class Z(val value: String?)

fun foo1(fs: (Z) -> Z) = fs(Z("O"))

fun box(): String {
    val t = foo1 { Z(it.value!! + "K") }
    if (t.value != "OK") return "Failed: t=$t"

    return "OK"
}
