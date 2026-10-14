// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
error class Foo
error class Bar

abstract class C {
    abstract fun memberFun(): String | Bar
    abstract val memberVal: String | Bar
}

fun test(
    a: String | Foo,
    b: C | Foo,
    c: Foo | Bar,
) {
    val x1 = a|.length
    val x2 = b|.memberFun()
    val x3 = b|.memberVal
    val x4 = c|.toString()
    val x5 = ""<!UNNECESSARY_SAFE_CALL!>|.<!>length
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, safeCall */
