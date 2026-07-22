// RUN_PIPELINE_TILL: BACKEND
// SCOPE_DUMP: F:equals, G:equals, I:equals, J:equals

interface A {
    fun a() = Unit
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean
}

interface B {
    fun b() = Unit
    override fun equals(@EqualityBound(B::class) other: Any?): Boolean
}

fun useSite_1(x: Any, y: Any) {
    if (x !is A || y !is B) return
    if (x != y) return
    y.a()
    y.b()
}

fun useSite_2(a: A, b: B, c: Any) {
    if (a == b) {
        b.a()
        if (<!DEBUG_INFO_EXPRESSION_TYPE("A & B")!>b<!> == c) {
            <!DEBUG_INFO_EXPRESSION_TYPE("A & B")!>c<!>.a()
            c.b()
        }
    }
    if (a == c && b == c) {
        c.a()
        c.b()
    }
    if (b == c && a == c) {
        c.a()
        c.b()
    }
}

open class C<out T> {
    override fun equals(@EqualityBound(C::class) other: Any?): Boolean = false
}
class D<T>(val d: T) : C<T>() {
    override fun equals(@EqualityBound(D::class) other: Any?): Boolean = false
}

fun <T> util(a: (T) -> Unit, b: (T) -> Unit, c: (T) -> Unit) = Unit

fun useSite_3(x: Any?) {
    util({ it: C<CharSequence> -> }, { it: D<*> -> }) { it ->
        if (<!DEBUG_INFO_EXPRESSION_TYPE("C<kotlin.CharSequence> & D<*>")!>it<!> == x) {
            x.d
        }
    }

    util({ it: D<*> -> }, { it: C<CharSequence> -> }) { it ->
        if (<!DEBUG_INFO_EXPRESSION_TYPE("D<*> & C<kotlin.CharSequence>")!>it<!> == x) {
            x.d
        }
    }
}

fun <T> useSite_4(t: T, a: Any?) where T : A, T : B {
    if (t == a) {
        a.a()
        a.b()
    }
}

abstract class E : A {
    fun e() = Unit
    override abstract fun equals(@EqualityBound(E::class) other: Any?): Boolean
}

abstract class F : E(), A
abstract class G : A, E()

fun useSite_5(a: Any?, f: F, g: G) {
    if (f == a) {
        a.a()
        a.e()
    }
    if (g == a) {
        a.a()
        a.e()
    }
}

fun <T : S?, S> useSite_6(t: T, a: Any?) where S : A, S : E {
    if (t == a) {
        a?.a()
        a?.e()
    }
}

fun <S, T : S> useSite_7(a: Any?, t: T) where S : E, S : A {
    if (t == a) {
        a.a()
        a.e()
    }
}

interface H {
    override fun equals(other: Any?): Boolean
}

interface I : H, A
interface J : A, H

fun useSite_8(a: Any?, b: A, c: H) {
    if (b is H && b == a) {
        a.a()
    }
    if (c is A && c == a) {
        a.a()
    }
}

interface D1 {
    fun d1() = Unit
    override fun equals(@EqualityBound(D1::class) other: Any?): Boolean
}

interface D2 : D1 {
    override fun equals(other: Any?): Boolean
}

interface D3 : D1 {
    override fun equals(other: Any?): Boolean
}

interface D4 {
    fun d4() = Unit
    override fun equals(@EqualityBound(D4::class) other: Any?): Boolean
}

fun <T, T2> useSite_9(a: Any?, t: T, t2: T2) where T : D2, T : D3, T2 : D2, T2 : D4 {
    if (t == a) {
        a.d1()
    }
    if (t2 == a) {
        a.d1()
        a.d4()
    }
}

/* GENERATED_FIR_TAGS: andExpression, capturedType, classDeclaration, classReference, disjunctionExpression,
equalityExpression, functionDeclaration, functionalType, ifExpression, interfaceDeclaration, intersectionType,
isExpression, lambdaLiteral, nullableType, operator, out, override, primaryConstructor, propertyDeclaration, smartcast,
starProjection, typeParameter */
