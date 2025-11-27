// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// ISSUE: KT-82737

class Box<T> {
    fun put(x: T) {}

    @Suppress("UNCHECKED_CAST")
    fun get() = null as T
}

fun <T> buildBox(block: Box<T>.() -> Unit) = Box<T>().apply(block)

fun tst() {
    buildBox {
        // expected String, top-level lambda

        val returnUnit: String <!INITIALIZER_TYPE_MISMATCH("String; () -> Unit")!>=<!> { }
        val returnWithOuterTv: String <!INITIALIZER_TYPE_MISMATCH("String; () -> Int")!>=<!> { get() }
        val returnInt: String <!INITIALIZER_TYPE_MISMATCH("String; () -> Int")!>=<!> { 42 }
        val returnUnitWithParam: String <!INITIALIZER_TYPE_MISMATCH("String; (??? (Uninferred type c: ConeTypeVariableTypeConstructor(_RP0))) -> Unit")!>=<!> { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> }
        val returnWithOuterTvWithParam: String <!INITIALIZER_TYPE_MISMATCH("String; (??? (Uninferred type c: ConeTypeVariableTypeConstructor(_RP0))) -> Int")!>=<!> { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> get() }
        val returnIntWithParam: String <!INITIALIZER_TYPE_MISMATCH("String; (??? (Uninferred type c: ConeTypeVariableTypeConstructor(_RP0))) -> Int")!>=<!> { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> 42 }
        val returnParam: String <!INITIALIZER_TYPE_MISMATCH("String; (??? (Uninferred type c: ConeTypeVariableTypeConstructor(_RP0))) -> ??? (Unknown type for value parameter it)")!>=<!> { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> it }

        put(1)
    }

    buildBox {
        // expected String, argument

        fun foo(str: String) {}

        foo <!ARGUMENT_TYPE_MISMATCH("() -> Unit; String")!>{ }<!>
        foo <!ARGUMENT_TYPE_MISMATCH("() -> Int; String")!>{ get() }<!>
        foo <!ARGUMENT_TYPE_MISMATCH("() -> Int; String")!>{ 42 }<!>
        foo <!ARGUMENT_TYPE_MISMATCH("(??? (Unknown lambda parameter type)) -> Unit; String")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> }<!>
        foo <!ARGUMENT_TYPE_MISMATCH("(??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type); String")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> get() }<!>
        foo <!ARGUMENT_TYPE_MISMATCH("(??? (Unknown lambda parameter type)) -> Int; String")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> 42 }<!>
        foo <!ARGUMENT_TYPE_MISMATCH("(??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type); String")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> it }<!>

        put(1)
    }

    buildBox {
        // top-level lambda

        { }
        { get() }
        { 42 }
        { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> }
        { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> get() }
        { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> 42 }
        <!CANNOT_INFER_IT_PARAMETER_TYPE!>{ <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> it }<!>

        put(1)
    }

    buildBox {
        // argument lambda

        fun foo(any: Any) {}

        foo { }
        foo { get() }
        foo { 42 }
        foo { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> }
        foo { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> get() }
        foo { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> 42 }
        foo { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> it }

        put(1)
    }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter, typeWithExtension */
