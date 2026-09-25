// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// RENDER_DIAGNOSTICS_FULL_TEXT
<!WRONG_MODIFIER_TARGET("error; class")!>error<!> class Foo

fun test(x: String | Foo, y: String? | Foo, z: String?) {
    x<!UNSAFE_CALL("on receiver of union type 'String | Foo'. Consider using a safe call '|.'")!>.<!>length

    y<!UNSAFE_CALL("on receiver of nullable union type 'String? | Foo'")!>.<!>length
    y<!UNSAFE_CALL("on receiver of nullable union type 'String? | Foo'")!>|.<!>length
    y<!UNSAFE_CALL("on receiver of nullable union type 'String? | Foo'")!>?.<!>length

    z<!UNSAFE_CALL("on receiver of nullable type 'String?'. Consider using a safe call '?.' or non-null asserted call '!!.'")!>.<!>length
    z<!UNSAFE_CALL("on receiver of nullable type 'String?'. Consider using a safe call '?.' or non-null asserted call '!!.'")!>|.<!>length

    x<!UNSAFE_CALL("on receiver of union type 'String | Foo'. Consider using a safe call '|.'")!>.<!>foo()

    y<!UNSAFE_CALL("on receiver of nullable union type 'String? | Foo'")!>.<!>foo()
    y<!UNSAFE_CALL("on receiver of nullable union type 'String? | Foo'")!>|.<!>foo()
    y<!UNSAFE_CALL("on receiver of nullable union type 'String? | Foo'")!>?.<!>foo()

    z<!UNSAFE_CALL("on receiver of nullable type 'String?'. Consider using a safe call '?.' or non-null asserted call '!!.'")!>.<!>foo()
    z<!UNSAFE_CALL("on receiver of nullable type 'String?'. Consider using a safe call '?.' or non-null asserted call '!!.'")!>|.<!>foo()
}

fun String.foo() {}

fun testLoop(x: List<String> | Foo, y: List<String>? | Foo, z: List<String>?) {
    for (e in <!ITERATOR_ON_NULLABLE("on receiver of union type 'List<String> | Foo'")!>x<!>) {}
    for (e in <!ITERATOR_ON_NULLABLE("on receiver of nullable union type 'List<String>? | Foo'")!>y<!>) {}
    for (e in <!ITERATOR_ON_NULLABLE("on receiver of nullable type 'List<String>?'")!>z<!>) {}

}

fun testInvoke(x: (() -> Unit) | Foo, y : (() -> Unit)? | Foo, z : (() -> Unit)?) {
    <!UNSAFE_IMPLICIT_INVOKE_CALL("on receiver of union type '() -> Unit | Foo'. Consider using a safe call '|.invoke'")!>x<!>()
    <!UNSAFE_IMPLICIT_INVOKE_CALL("on receiver of nullable union type '(() -> Unit)? | Foo'")!>y<!>()
    <!UNSAFE_IMPLICIT_INVOKE_CALL("on receiver of nullable type '(() -> Unit)?'. Consider using a safe call '?.invoke' or non-null asserted call '!!()'")!>z<!>()
}

class A {
    infix fun foo(s: String) {}
    operator fun plus(s: String) {}
}

infix fun A.bar(s: String) {}
operator fun A.div(s: String) {}

fun testInfix(x: A | Foo, y: A? | Foo, z: A?) {
    x <!UNSAFE_INFIX_CALL("on receiver of union type 'A | Foo'. Consider using a safe call '|.'")!>foo<!> ""
    x <!UNSAFE_INFIX_CALL("on receiver of union type 'A | Foo'. Consider using a safe call '|.'")!>bar<!> ""
    y <!UNSAFE_INFIX_CALL("on receiver of nullable union type 'A? | Foo'")!>foo<!> ""
    y <!UNSAFE_INFIX_CALL("on receiver of nullable union type 'A? | Foo'")!>bar<!> ""
    z <!UNSAFE_INFIX_CALL("on receiver of nullable type 'A?'. Consider using a safe call '?.' or non-null asserted call '!!.'")!>foo<!> ""
    z <!UNSAFE_INFIX_CALL("on receiver of nullable type 'A?'. Consider using a safe call '?.' or non-null asserted call '!!.'")!>bar<!> ""
}

fun testOperator(x: A | Foo, y: A? | Foo, z: A?) {
    x <!UNSAFE_OPERATOR_CALL("on receiver of union type 'A | Foo'. Consider using a safe call '|.'")!>+<!> ""
    x <!UNSAFE_OPERATOR_CALL("on receiver of union type 'A | Foo'. Consider using a safe call '|.'")!>/<!> ""
    y <!UNSAFE_OPERATOR_CALL("on receiver of nullable union type 'A? | Foo'")!>+<!> ""
    y <!UNSAFE_OPERATOR_CALL("on receiver of nullable union type 'A? | Foo'")!>/<!> ""
    z <!UNSAFE_OPERATOR_CALL("on receiver of nullable type 'A?'. Consider using a safe call '?.' or non-null asserted call '!!.'")!>+<!> ""
    z <!UNSAFE_OPERATOR_CALL("on receiver of nullable type 'A?'. Consider using a safe call '?.' or non-null asserted call '!!.'")!>/<!> ""
}

fun testCallableReference(x: String | Foo, y: String? | Foo, z: String?) {
    x::<!UNSAFE_CALLABLE_REFERENCE("on receiver of union type 'String | Foo'")!>length<!>
    x::<!UNSAFE_CALLABLE_REFERENCE("on receiver of union type 'String | Foo'")!>foo<!>
    y::<!UNSAFE_CALLABLE_REFERENCE("on receiver of nullable union type 'String? | Foo'")!>length<!>
    y::<!UNSAFE_CALLABLE_REFERENCE("on receiver of nullable union type 'String? | Foo'")!>foo<!>
    z::<!UNSAFE_CALLABLE_REFERENCE("on receiver of nullable type 'String?'")!>length<!>
    z::<!UNSAFE_CALLABLE_REFERENCE("on receiver of nullable type 'String?'")!>foo<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, forLoop, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, localProperty, multiplicativeExpression, nullableType, operator, propertyDeclaration, safeCall,
stringLiteral */
