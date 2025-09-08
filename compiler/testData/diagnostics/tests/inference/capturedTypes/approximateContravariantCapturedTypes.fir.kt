// RUN_PIPELINE_TILL: FRONTEND
class Foo<T : Number>(var x: T) {
    fun setX1(y: T): T {
        this.x = y
        return y
    }
}

fun <T : Number> Foo<T>.setX(y: T): T {
    return y
}

class Foo2<T>(var x: T) {
    fun setX1(y: T): T {
        this.x = y
        return y
    }
}

fun <T> Foo2<T>.setX(y: T): T {
    this.x = y
    return y
}

fun Float.bar() {}

fun <K> id(x: K) = x

fun test1() {
    val fooSetRef = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo<*>, @ParameterName(...) CapturedType(*), CapturedType(*)>")!>Foo<*>::setX<!>

    val fooSetRef2 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo<*>, *, kotlin.Number>")!>id(
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo<*>, @ParameterName(...) CapturedType(*), CapturedType(*)>")!>Foo<*>::setX<!>
    )<!>
    val foo = Foo<Float>(1f)

    fooSetRef.invoke(foo, <!MEMBER_PROJECTED_OUT!>1<!>)
    fooSetRef2.invoke(foo, <!MEMBER_PROJECTED_OUT!>1<!>)

    foo.x.bar()
}

fun test2() {
    val fooSetRef = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo<*>, @ParameterName(...) CapturedType(*), CapturedType(*)>")!>Foo<*>::setX1<!>
    val fooSetRef2 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo<*>, *, kotlin.Number>")!>id(
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo<*>, @ParameterName(...) CapturedType(*), CapturedType(*)>")!>Foo<*>::setX1<!>
    )<!>
    val foo = Foo<Float>(1f)

    fooSetRef.invoke(foo, <!MEMBER_PROJECTED_OUT!>1<!>)
    fooSetRef2.invoke(foo, <!MEMBER_PROJECTED_OUT!>1<!>)

    foo.x.bar()
}

fun test3() {
    val fooSetRef = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo2<*>, @ParameterName(...) CapturedType(*), CapturedType(*)>")!>Foo2<*>::setX<!>
    val fooSetRef2 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo2<*>, *, kotlin.Any?>")!>id(
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo2<*>, @ParameterName(...) CapturedType(*), CapturedType(*)>")!>Foo2<*>::setX<!>
    )<!>
    val foo = Foo2<Int>(1)

    fooSetRef.invoke(foo, <!MEMBER_PROJECTED_OUT!>""<!>)
    fooSetRef2.invoke(foo, <!MEMBER_PROJECTED_OUT!>""<!>)

    foo.x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()
}

fun test4() {
    val fooSetRef = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo2<*>, @ParameterName(...) CapturedType(*), CapturedType(*)>")!>Foo2<*>::setX1<!>
    val fooSetRef2 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo2<*>, *, kotlin.Any?>")!>id(
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo2<*>, @ParameterName(...) CapturedType(*), CapturedType(*)>")!>Foo2<*>::setX1<!>
    )<!>
    val foo = Foo2<Int>(1)

    fooSetRef.invoke(foo, <!MEMBER_PROJECTED_OUT!>""<!>)
    fooSetRef2.invoke(foo, <!MEMBER_PROJECTED_OUT!>""<!>)

    foo.x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()
}

/* GENERATED_FIR_TAGS: assignment, callableReference, capturedType, classDeclaration, funWithExtensionReceiver,
functionDeclaration, integerLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration,
starProjection, stringLiteral, thisExpression, typeConstraint, typeParameter */
