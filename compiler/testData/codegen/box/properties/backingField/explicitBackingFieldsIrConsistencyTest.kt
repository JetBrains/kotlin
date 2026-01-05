// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ExplicitBackingFields
// DUMP_IR
// WITH_STDLIB
// ISSUE: KT-83269

@kotlin.jvm.JvmInline
value class V(val x: Int)

class A {
    val p: Any
        field = V(1)

    fun foo(): Int = p.x
}

fun box() = "OK".also { A().foo() }
