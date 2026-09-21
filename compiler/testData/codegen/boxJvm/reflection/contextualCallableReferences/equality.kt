// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

context(c: String)
fun topFun(x: Int): String = c + x

fun box(): String {
    val bound: KFunction<String> = context("ctx") { ::topFun }
    val unbound = bound.javaMethod!!.kotlinFunction!!

    if (unbound.call("ctx", 1) != bound.call(1)) return "FAIL 1: unbound and bound disagree"

    if (unbound == bound) return "FAIL 2: unbound callable is equal to a context-bound reference"

    return "OK"
}
