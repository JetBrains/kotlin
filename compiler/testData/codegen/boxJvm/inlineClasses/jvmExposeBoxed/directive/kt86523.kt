// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

@JvmInline
value class IC(val c: String)

class Test(val v: IC = IC("OK"), b: Boolean = true, c: Boolean = b)

fun box(): String {
    return Test().v.c
}
