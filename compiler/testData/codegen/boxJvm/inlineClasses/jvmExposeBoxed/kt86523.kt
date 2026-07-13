// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val c: String)

@JvmExposeBoxed
class Test(val v: IC = IC("OK"), b: Boolean = true, c: Boolean = b)

fun box(): String {
    return Test().v.c
}
