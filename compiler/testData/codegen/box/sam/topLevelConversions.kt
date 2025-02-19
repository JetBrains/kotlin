// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-67869
// LANGUAGE: +ResolveTopLevelLambdasAsSyntheticCallArgument
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

fun interface MyFun {
    fun foo(x: String): Int
}

val topLevel: MyFun = { it.length }

fun baz(x: MyFun = { it.length }): MyFun = x

class A(
    val classMember: MyFun = { it.length }
)

fun returnExpr(): MyFun = { it.length }

fun returnExplicit(): MyFun {
    return { it.length }
}

val withGetter: MyFun
    get() = { it.length }

fun box(): String {
    if (topLevel.foo("OK") != 2) return "fail"
    if (baz().foo("OK") != 2) return "fail"
    if (A().classMember.foo("OK") != 2) return "fail"
    if (returnExpr().foo("OK") != 2) return "fail"
    if (returnExplicit().foo("OK") != 2) return "fail"
    if (withGetter.foo("OK") != 2) return "fail"

    var local: MyFun = { it.length }
    if (local.foo("OK") != 2) return "fail"
    local = { it.length + 1 }
    if (local.foo("OK") != 3) return "fail"

// TODO: Uncomment it once KT-74899 is fixed
//    val whenResult: MyFun = when {
//        "0".length == 1 -> {
//            { it.length}
//        }
//        else -> {
//            { it.length - 1 }
//        }
//    }
//
//    if (whenResult.foo("OK") != 2) return "fail"

    return "OK"
}
