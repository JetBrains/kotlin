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
    a.test1()
    a.x
    A()
    A("", "")
    with("context") {
        a.test2()
        a.e
        Unit
    }
    with(a) {
        "".test3()
        "".f
        Unit
    }
    a.test4()
    a.c
    a.d
    a.d = ""
    a.g
    a.h
    A.MyTypealias
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, integerLiteral, lambdaLiteral, lateinit, primaryConstructor, propertyDeclaration,
propertyDeclarationWithContext, propertyDelegate, propertyWithExtensionReceiver, secondaryConstructor, setter,
stringLiteral, thisExpression, typeAliasDeclaration */
