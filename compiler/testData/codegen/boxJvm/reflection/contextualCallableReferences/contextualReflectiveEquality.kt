// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.jvm.internal.Reflection
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction

context(c: String)
fun topFun(x: Int): String = c + x

fun box(): String {
    val pkg = Reflection.getOrCreateKotlinPackage(object {}::class.java.enclosingClass)
    val u1 = pkg.members.single { it.name == "topFun" }
    val u2 = pkg.members.single { it.name == "topFun" }
    if (u1 != u2) return "FAIL 1: two enumerations of the same unbound contextual function are not equal"
    if (u1.hashCode() != u2.hashCode()) return "FAIL 2: equal unbound callables have different hashCodes"

    val bound: KFunction<String> = context("ctx") { ::topFun }
    // sanity: both denote the same function
    if ((u1 as KFunction<*>).call("ctx", 1) != bound.call(1)) return "FAIL 3: unbound and bound disagree"

    // the unbound callable has no bound context arguments, the reference has ["ctx"] => not equal
    if (u1 == (bound as KCallable<*>)) return "FAIL 4: unbound callable is equal to a context-bound reference"

    return "OK"
}
