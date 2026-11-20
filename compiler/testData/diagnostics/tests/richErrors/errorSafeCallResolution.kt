// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
error class Foo
error class Bar

abstract class C {
    abstract fun memberFun(): String | Bar
    abstract val memberVal: String | Bar
}

fun C.extensionFun(): String | Bar = ""

fun <T : C | Foo> test(
    a: String | Foo,
    b: C | Foo,
    c: Foo | Bar,
    d: T,
) {
    val x1 = a|.length
    val x2 = b|.memberFun()
    val x3 = b|.memberVal
    val x4 = c|.toString() // resolves to the extension on Any?
    val x4_ = c|.<!UNRESOLVED_REFERENCE!>hashCode<!>()
    val x5 = ""<!UNNECESSARY_SAFE_CALL!>|.<!>length
    val x6 = d|.memberFun()
    val x7 = d|.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>extensionFun<!>() // TODO(KT-89099)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, safeCall */
