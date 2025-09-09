// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters, +NestedTypeAliases
// WITH_STDLIB

@MustUseReturnValues
class A(val x: String = "x") {
    constructor(a: String, b: String = "x") : this(a)

    fun test1(): String = "1"

    context(a: String)
    fun test2(): String {
        return a
    }

    fun String.test3(): String {
        return this
    }

    @IgnorableReturnValue
    fun test4(): Int = 1

    val c: String
        get() = ""

    var d: String = ""
        get() = ""
        set(value) { field = value + "" }

    context(a: String)
    val e: String
        get() = ""

    val String.f: String
        get() = ""

    lateinit var g: String

    val h: String by lazy { "" }

    typealias MyTypealias = String
}

fun usage(a: A) {
    a.<!RETURN_VALUE_NOT_USED!>test1<!>()
    a.<!RETURN_VALUE_NOT_USED!>x<!>
    <!RETURN_VALUE_NOT_USED!>A<!>()
    <!RETURN_VALUE_NOT_USED!>A<!>("", "")
    with("context") {
        a.<!RETURN_VALUE_NOT_USED!>test2<!>()
        a.<!RETURN_VALUE_NOT_USED!>e<!>
        Unit
    }
    with(a) {
        "".<!RETURN_VALUE_NOT_USED!>test3<!>()
        "".<!RETURN_VALUE_NOT_USED!>f<!>
        Unit
    }
    a.test4()
    a.<!RETURN_VALUE_NOT_USED!>c<!>
    a.<!RETURN_VALUE_NOT_USED!>d<!>
    a.d = ""
    a.<!RETURN_VALUE_NOT_USED!>g<!>
    a.<!RETURN_VALUE_NOT_USED!>h<!>
    <!UNUSED_EXPRESSION!>A.MyTypealias<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, integerLiteral, lambdaLiteral, lateinit, primaryConstructor, propertyDeclaration,
propertyDeclarationWithContext, propertyDelegate, propertyWithExtensionReceiver, secondaryConstructor, setter,
stringLiteral, thisExpression, typeAliasDeclaration */
