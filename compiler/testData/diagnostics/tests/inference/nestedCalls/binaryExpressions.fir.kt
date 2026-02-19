// RUN_PIPELINE_TILL: FRONTEND
package h

interface A<T> {}

fun <T> newA(): A<T> = throw Exception()

interface Z

fun <T> id(t: T): T = t

//binary expressions
//identifier
infix fun <T> Z.foo(a: A<T>): A<T> = a

fun test(z: Z) {
    z <!CANNOT_INFER_PARAMETER_TYPE!>foo<!> <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>()
    val a: A<Int> = id(z foo newA())
    val b: A<Int> = id(z.foo(newA()))
    use(a, b)
}

//binary operation expression
operator fun <T> Z.plus(a: A<T>): A<T> = a

fun test1(z: Z) {
    <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(z <!CANNOT_INFER_PARAMETER_TYPE!>+<!> <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>())
    val a: A<Z> = z + newA()
    val b: A<Z> = z.plus(newA())
    val c: A<Z> = id(z + newA())
    val d: A<Z> = id(z.plus(newA()))
    use(a, b, c, d)
}

//comparison operation
operator fun <T> Z.compareTo(a: A<T>): Int { use(a); return 1 }

fun test2(z: Z) {
    val a: Boolean = id(z <!CANNOT_INFER_PARAMETER_TYPE!><<!> <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>())
    val b: Boolean = id(z < newA<Z>())
    use(a, b)
}

//'equals' operation
fun Z.equals(any: Any): Int { use(any); return 1 }

fun test3(z: Z) {
    z == <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>()
    z == newA<Z>()
    <!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>id<!>(z == <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>())
    id(z == newA<Z>())

    <!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>id<!>(z === <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>())
    id(z === newA<Z>())
}

//'in' operation
fun test4(collection: Collection<A<*>>) {
    id(<!CANNOT_INFER_PARAMETER_TYPE!>newA<!>() in collection)
    id(newA<Int>() in collection)
}

//boolean operations
fun <T> toBeOrNot(): Boolean = throw Exception()

fun test5() {
    if (<!CANNOT_INFER_PARAMETER_TYPE!>toBeOrNot<!>() && <!CANNOT_INFER_PARAMETER_TYPE!>toBeOrNot<!>()) {}
    if (toBeOrNot<Int>() && toBeOrNot<Int>()) {}
}

//use
fun use(vararg a: Any?) = a

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, comparisonExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, infix, integerLiteral, interfaceDeclaration, localProperty,
nullableType, operator, outProjection, propertyDeclaration, starProjection, typeParameter, vararg */
