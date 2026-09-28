// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
<!WRONG_MODIFIER_TARGET!>error<!> class Foo
<!WRONG_MODIFIER_TARGET!>error<!> class Bar

abstract class C {
    abstract fun memberFun(): String | Bar
    abstract val memberVal: String | Bar
}

fun test(
    a: String | Foo,
    b: C | Foo,
    c: Foo | Bar,
) {
    val x1 = <!UNNECESSARY_SAFE_CALL!>a|.length<!>
    val x2 = <!UNNECESSARY_SAFE_CALL!>b|.memberFun()<!>
    val x3 = <!UNNECESSARY_SAFE_CALL!>b|.memberVal<!>
    val x4 = <!UNNECESSARY_SAFE_CALL!>c|.toString()<!>
    val x5 = <!UNNECESSARY_SAFE_CALL!>""|.length<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, safeCall */
