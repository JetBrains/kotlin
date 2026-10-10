// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: INDY
// ISSUE: KT-89915

import java.util.Optional

@JvmInline
value class A(val x: Any)

fun box(): String {
    val a = Optional.of(A("a")).map(fun(v: A): Any = v.x).get()
    if (a != "a") return "Fail A: $a"

    val a2 = Optional.of(A("a")).map { it.x }.get()
    if (a2 != "a") return "Fail A (lambda): $a2"

    val r = Optional.of(Result.success("a")).map { r: Result<String> -> r.getOrNull() }.get()
    if (r != "a") return "Fail Result: $r"

    val r2 = Optional.of(Result.success("a")).map { it.getOrThrow() }.get()
    if (r2 != "a") return "Fail Result (implicit it): $r2"

    return "OK"
}
