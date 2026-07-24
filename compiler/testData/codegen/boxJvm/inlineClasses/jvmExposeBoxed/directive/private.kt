// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED
// TARGET_BACKEND: JVM_IR

@JvmInline
value class IC(val c: String)

private fun foo(ic: IC): String = ic.c

private class C {
    class D {
        fun foo(ic: IC): String = ic.c
    }
}

fun box(): String = foo(IC("O")) + C.D().foo(IC("K"))
