import kotlin.reflect.KFunction1

// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun foo(i: Int) {}
fun foo(s: String) {}
fun foo2(i: Int) {}
fun foo3(i: Number) {}
fun <K> id(x: K): K = x
fun <K> id1(x: K): K = x
fun <L> id2(x: L): L = x
fun <T> baz1(x: T, y: T): T = TODO()
fun <T> baz2(x: T, y: Inv<T>): T = TODO()
fun <T> select(vararg x: T) = x[0]

fun <T, R> takeInterdependentLambdas(x: (T) -> R, y: (R) -> T) {}

fun <T> takeDependentLambdas(x: (T) -> Int, y: (Int) -> T) {}

class Inv<T>(val x: T)

fun test1() {
    val x1: (Int) -> Unit = id(id(::foo))
    val x2: (Int) -> Unit = baz1(id(::foo), ::foo)
    val x3: (Int) -> Unit = baz1(id(::foo), id(id(::foo)))
    val x4: (String) -> Unit = baz1(id(::foo), id(id(::foo)))

    id<(Int) -> Unit>(id(id(::foo)))
    id(id<(Int) -> Unit>(::foo))
    baz1<(Int) -> Unit>(id(::foo), id(id(::foo)))
    baz1(id(::foo), id(id<(Int) -> Unit>(::foo)))
    baz1(id(::foo), id<(Int) -> Unit>(id(::foo)))

    baz1(id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!>.inv() }, id<(Int) -> Unit> { })
    baz1(id1 { x -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id2 { x: Int -> })
    baz1(id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id2 { x: Int -> })

    baz2(id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id2(Inv { x: Int -> }))

    select(id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id1 { x: Number -> TODO() }, id1(id2 { x: Int -> x }))

    select(id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id1 { x: Number -> TODO() }, id1(id2(::foo2)))
    select(id1 { x: Inv<out Number> -> TODO() }, id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id1 { x: Inv<Int> -> TODO() })

    select(id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, id1 { x: Inv<Number> -> TODO() }, id1 { x: Inv<Int> -> TODO() })

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Int, kotlin.Unit>")!>select(id1(::foo), id(::foo3), id1(id2(::foo2)))<!>

    select({ <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, { x: Int -> TODO() })

    // Interdependent postponed arguments are unsupported
    takeInterdependentLambdas({}, {})
    takeInterdependentLambdas({ it }, { 10 })
    takeInterdependentLambdas({ 10 }, { it })
    takeInterdependentLambdas({ 10 }, { x -> x })
    takeInterdependentLambdas({ x -> 10 }, { it })
    takeInterdependentLambdas({ it }, { x -> 10 })

    takeDependentLambdas({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { it })
    takeDependentLambdas({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>it<!>.length }, { "it" })
    takeDependentLambdas({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>it<!>; 10 }, { })

    select({ <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, fun(x: Int) = 1)

    val x5: (Int) -> Unit = select({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { x: Number -> Unit })
    val x6 = select(id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, id(id<(Int) -> Unit> { x: Number -> Unit }))

    select(id(id2 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }), id(id { x: Int -> x }))

    val x7: (Int) -> Unit = selectNumber(id {}, id {}, id {})
    val x8: (Int) -> Unit = selectNumber(id { x -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x<!> }, id { x -> }, id { x -> })
    val x9: (Int) -> Unit = selectNumber(id { }, id { x -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>it<!> })

    val x10: (Int) -> Unit = selectFloat(id { }, id { x -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> })

    val x11: (B) -> Unit = selectC(id {  }, id { x -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> })

    /*
     * Upper constraint is less specific than lower (it's error):
     * K <: (A) -> Unit -> TypeVariable(_RP1) >: A
     * K >: (C) -> TypeVariable(_R) -> TypeVariable(_RP1) <: C
     */
    val x12 = <!INAPPLICABLE_CANDIDATE!>selectC<!>(id { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, id { x: B -> })
    val x13 = <!INAPPLICABLE_CANDIDATE!>selectA<!>(id { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, id { x: C -> })

    // one upper constraint and one lower
    val x14 = selectC(id { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, id { x: A -> }, { x -> x })
    val x15 = selectC(id { <!DEBUG_INFO_EXPRESSION_TYPE("C")!>it<!> }, { x: A -> }, id { x -> x })

    /*
     * Two upper constraints and one lower
     * K <: (C) -> Unit -> TypeVariable(_RP1) >: C
     * K <: (B) -> Unit -> TypeVariable(_RP1) >: B
     * K >: (A) -> TypeVariable(_R) -> TypeVariable(_RP1) <: A
     * K == intersect(CST(C,  B), A) == A
     */
    val x16: (C) -> Unit = selectB(id { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, { x -> }, id { x: A -> x })

    /*
     * two upper constraints and one equality (it's error)
     * K <: (C) -> Unit -> TypeVariable(_RP1) >: C
     * K == (B) -> Unit -> TypeVariable(_RP1) == B
     */
    val x17: (C) -> Unit = selectB(id { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, id { <!UNRESOLVED_REFERENCE!>it<!> }, id<(B) -> Unit> { x -> x })
    val x18: (C) -> Unit = select(id { <!DEBUG_INFO_EXPRESSION_TYPE("C")!>it<!> }, { <!DEBUG_INFO_EXPRESSION_TYPE("C")!>it<!> }, id<(B) -> Unit> { x -> x })

    val x19: String.() -> Unit = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id {}<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id(fun(x: String) {})<!>)
    val x20: String.() -> Unit = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>{}<!>, (fun(x: String) {}))
    val x21: String.() -> Unit = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id(fun(x: String) {})<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id(fun(x: String) {})<!>)
    val x22 = select(id<String.() -> Unit>(fun(x: String) {}), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id(fun(x: String) {})<!>)
    val x23 = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function2<kotlin.String, kotlin.String, kotlin.Unit>")!>id(fun String.(x: String) {})<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function2<kotlin.String, kotlin.String, kotlin.Unit>")!>id(fun(x: String, y: String) {})<!>)
    val x24 = select(id(fun String.(x: String) {}), id(fun(x: String, y: String) { }), { x: String -> this })
    val x25 = select(id(fun String.(x: String) {}), id(fun(x: String, y: String) { }), { x: String, y: String -> x })

    // It isn't related with posponed arguments, see KT-38439
    val x26: Int.(String) -> Int = fun (x: String) = 10
    val x27: Int.(String) -> Int = id(fun (x: String) = 10)

    val x28 = select(id { x, y -> x.<!UNRESOLVED_REFERENCE!>inv<!>() + y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, { x: Int, y -> y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, { x, y: Number -> x.inv() })
    val x29 = select(id { x, y -> x.<!UNRESOLVED_REFERENCE!>inv<!>() + y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, id { x: Int, y -> y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, id { x, y: Number -> x.<!UNRESOLVED_REFERENCE!>inv<!>() })
    val x30 = select({ x, y -> x.<!UNRESOLVED_REFERENCE!>inv<!>() + y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, id { x: Int, y -> y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, id { x, y: Number -> x.<!UNRESOLVED_REFERENCE!>inv<!>() })

    val x31 = select(
        id { x, y -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.inv() + <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>y<!>.toByte() },
        id<(Int, Number) -> Int> { x, y -> x.inv() },
        {} as (Number, Number) -> Int
    )

    val x32 = selectPosponedArgument({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> }, { x: Int -> }, { x: Nothing -> x })
    val x33 = selectPosponedArgument({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> }, { } as (Int) -> Unit, { x: Nothing -> x })
    val x34 = selectPosponedArgument({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> }, { } as (Nothing) -> Unit, { x: Int -> x })
    val x35 = selectPosponedArgument({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> }, { } as (Int) -> Unit, { } as (Nothing) -> Unit)

    val x36 = selectPosponedArgument3({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { x: Int -> x })
    val x37 = selectPosponedArgument3({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { x: Number -> x }, { x: Int -> x })
    val x38 = selectPosponedArgument3Revert({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { x: Int -> x })
    val x39 = selectPosponedArgument3Revert({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>it<!> }, { x: Number -> x }, { x: Int -> x })

    val x40 = select(id<Int.(String) -> Unit> {}, { x: Int, y: String -> x })

    val x41 = select(A2(), { a, b, c -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!>; <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>b<!>; <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>c<!> })
    val x42 = select(A3(), { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!> })
    val x43 = select(A3(), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>)
    val x44 = select(A3(), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>, { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!> }, { it -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>it<!> })

    val x45 = select(A4(), { x: Number -> "" })
    val x46 = select(A5<Int, Int>(), { x: Number, y: Int -> "" })

    val x47 = select(A2(), id { a, b, c -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!>; <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>b<!>; <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>c<!> })
    val x48 = select(id(A3()), { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!> })
    val x49 = select(A3(), id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>))
    val x50 = select(A3(), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>, { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!> }, { it -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>it<!> })

    val x51 = select(A4(), id { x: Number -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x<!> })
    val x52 = select(id(A5<Int, Int>()), id { x: Number, y: Int -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x<!>;<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>y<!> })
    val x53 = select(id(A5<Int, Int>()), id { x, y -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>;<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>y<!> })
    val x54 = select(id(<!DEBUG_INFO_EXPRESSION_TYPE("A5<kotlin.Number, kotlin.Int>")!>A5()<!>), id { x: Number, y: Int -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x<!>;<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>y<!> })
    val x55: Function2<Number, Int, Float> = select(id(A5()), id { x, y -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>;<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>y<!>; 1f })
}

fun <T: (Number) -> Unit> selectNumber(arg1: T, arg2: T, arg3: T) = arg1

fun <T: (Float) -> Unit> selectFloat(arg1: T, arg2: T, arg3: T) = arg2

fun <T: (A) -> Unit> selectA(arg1: T, arg2: T, arg3: T) = arg2
fun <T: (B) -> Unit> selectB(arg1: T, arg2: T, arg3: T) = arg2
fun <T: (C) -> Unit> selectC(arg1: T, arg2: T, arg3: T) = arg2

fun <T> selectPosponedArgument(vararg x: (T) -> Unit) = x[0]
fun <T : R, R : L, L> selectPosponedArgument3(x: (T) -> Unit, y: (R) -> Unit, z: (L) -> Unit) = x
fun <T, R: T, L: R> selectPosponedArgument3Revert(x: (T) -> Unit, y: (R) -> Unit, z: (L) -> Unit) = x

interface A
class B: A
class C: A

class A2: Function3<Int, String, Float, Float> {
    override fun invoke(p1: Int, p2: String, p3: Float): Float = 4f
}

class A3: KFunction1<Number, String> {
    override fun invoke(p1: Number): String = TODO()
    override val name: String = TODO()

    fun foo1(x: Int) {}
    fun foo1(x: Any?) {}
}

class A4: Function1<Int, Float> {
    override fun invoke(p1: Int): Float = TODO()
}

class A5<K, Q>: Function2<K, Q, Float> {
    override fun invoke(p1: K, p2: Q): Float = 5f
}
