// !LANGUAGE: +InlineClasses

// FILE: utils.kt

inline class Result<T>(val a: Any?) {
    fun typed(): T = a as T
}

// FILE: test.kt

fun <K> materialize(): K = TODO()

fun test(asInt: Result<Int>, asString: Result<String>, asResult: Result<Result<Int>>) {
    val a1 = materialize<Result<Int>>() // unbox
    val a2 = materialize<Result<Result<Int>>>() // unbox

    val b1 = asInt.typed() // intValue
    val b2 = asString.typed()

    val c1 = asResult.typed() // unbox

    materialize<Result<Int>>()
    asInt.typed()
    asString.typed()
    asResult.typed()
}

// @TestKt.class:
// 0 INVOKESTATIC Result\$Erased.box
// 0 INVOKESTATIC Result\.box
// 3 INVOKEVIRTUAL Result.unbox

// 0 valueOf
// 1 intValue