// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY

inline class Z(val value: Any?)

fun foo1(fs: (Z) -> Z) = fs(Z(1))

fun box(): String {
    val t = foo1 { Z((it.value as Int) + 41) }
    if (t.value != 42) return "Failed: t=$t"

    return "OK"
}
