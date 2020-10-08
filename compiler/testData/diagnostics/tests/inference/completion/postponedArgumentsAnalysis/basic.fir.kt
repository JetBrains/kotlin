// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNCHECKED_CAST

import kotlin.reflect.KFunction1

fun withOverload(i: Int) {}
fun withOverload(s: String) {}

fun takeInt(i: Int) {}
fun takeNumber(i: Number) {}

fun <K> id(x: K): K = x
fun <K> id1(x: K): K = x
fun <L> id2(x: L): L = x

fun <T> selectWithInv(x: T, y: Inv<T>): T = TODO()
fun <T> select(vararg x: T) = x[0]

fun <T> takeLambdas(vararg x: (T) -> Unit) = x[0]
fun <T : R, R : L, L> takeLambdasWithDirectlyDependentTypeParameters(x: (T) -> Unit, y: (R) -> Unit, z: (L) -> Unit) = x
fun <T, R: T, L: R> takeLambdasWithInverselyDependentTypeParameters(x: (T) -> Unit, y: (R) -> Unit, z: (L) -> Unit) = x

fun <T, R> takeInterdependentLambdas(x: (T) -> R, y: (R) -> T) {}

fun <T> takeDependentLambdas(x: (T) -> Int, y: (Int) -> T) {}

class Inv<T>(val x: T)

fun <T: (Number) -> Unit> selectNumber(arg1: T, arg2: T, arg3: T) = arg1

fun <T: (Float) -> Unit> selectFloat(arg1: T, arg2: T, arg3: T) = arg2

fun <T: (A) -> Unit> selectA(arg1: T, arg2: T, arg3: T) = arg2
fun <T: (B) -> Unit> selectB(arg1: T, arg2: T, arg3: T) = arg2
fun <T: (C) -> Unit> selectC(arg1: T, arg2: T, arg3: T) = arg2

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

    companion object {
        fun foo2(x: Int) {}
        fun foo2(x: Any?) {}
    }
}

class A4: Function1<Int, Float> {
    override fun invoke(p1: Int): Float = TODO()
}

class A5<K, Q>: Function2<K, Q, Float> {
    override fun invoke(p1: K, p2: Q): Float = 5f
}

fun main() {
    // Inferring lambda parameter types by other lambda explicit parameters; expected type is type variable
    select(id1 { x -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id2 { x: Int -> })
    select(id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id2 { x: Int -> })
    selectWithInv(id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id2(Inv { x: Int -> }))
    select(id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id1 { x: Number -> TODO() }, id1(id2 { x: Int -> x }))
    select(id1 { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!>.inv() }, id1 { x: Number -> TODO() }, id1(id2(::takeInt)))
    select(id1 { x: Inv<out Number> -> TODO() }, id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, id1 { x: Inv<Int> -> TODO() })
    select(id1 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, id1 { x: Inv<Number> -> TODO() }, id1 { x: Inv<Int> -> TODO() })
    select(id(id2 { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }), id(id { x: Int -> x }))

    // Disambiguating callable references by other callable references without overloads
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Int, kotlin.Unit>")!>select(id(::withOverload), id(::takeInt), id(id(::takeNumber)))<!>

    // Interdependent lambdas by input-output types aren't supported
    takeInterdependentLambdas({}, {})
    takeInterdependentLambdas({ it }, { 10 })
    takeInterdependentLambdas({ 10 }, { it })
    takeInterdependentLambdas({ 10 }, { x -> x })
    takeInterdependentLambdas({ x -> 10 }, { it })
    takeInterdependentLambdas({ it }, { x -> 10 })

    // Dependent lambdas by input-output types
    takeDependentLambdas({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { it })
    takeDependentLambdas({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>it<!>.length }, { "it" })
    takeDependentLambdas({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>it<!>; 10 }, { })

    // Inferring lambda parameter types by anonymous function parameters
    select({ <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, fun(x: Int) = 1)

    // Inferring lambda parameter types by other lambda explicit parameters (lower constraints) and expected type (upper constraints)
    val x5: (Int) -> Unit = select({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { x: Number -> Unit })

    // Inferring lambda parameter types by other lambda explicit parameters (lower constraints) and specified type arguments (equality constraints)
    select(id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, id(id<(Int) -> Unit> { x: Number -> Unit }))

    // Inferring lambda parameter types by specified type arguments (equality constraints) of other lambdas
    select(id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!>.inv() }, id<(Int) -> Unit> { })
    select(
        id { x, y -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.inv() + <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>y<!>.toByte() },
        id<(Int, Number) -> Int> { x, y -> x.inv() },
        {} as (Number, Number) -> Int
    )

    // Inferring lambda parameter types by a few expected types (a few upper constraints)
    val x7: (Int) -> Unit = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Number, kotlin.Unit>")!>selectNumber(id {}, id {}, id {})<!>
    val x8: (Int) -> Unit = selectNumber(id { x -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x<!> }, id { x -> }, id { x -> })
    val x9: (Int) -> Unit = selectNumber(id { }, id { x -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>it<!> })
    val x10: (Int) -> Unit = selectFloat(id { }, id { x -> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> })
    val x11: (B) -> Unit = selectC(id {  }, id { x -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!> }, id { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> })

    // Inferring lambda parameter types by expected types (upper constraints) and other lambda explicit parameters (lower constraints)
    /*
     * Upper constraint is less specific than lower (it's error):
     * K <: (A) -> Unit -> TypeVariable(_RP1) >: A
     * K >: (C) -> TypeVariable(_R) -> TypeVariable(_RP1) <: C
     */
    val x12 = <!INAPPLICABLE_CANDIDATE!>selectC<!>(id { <!DEBUG_INFO_EXPRESSION_TYPE("C")!>it<!> }, id { x: B -> })
    val x13 = <!INAPPLICABLE_CANDIDATE!>selectA<!>(id { <!DEBUG_INFO_EXPRESSION_TYPE("A")!>it<!> }, id { x: C -> })
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

    // Inferring lambda parameter types by expected types (upper constraints) and specified type arguments (equality constraints) of other lambdas
    /*
     * two upper constraints and one equality (it's error)
     * K <: (C) -> Unit -> TypeVariable(_RP1) >: C
     * K == (B) -> Unit -> TypeVariable(_RP1) == B
     */
    val x17: (C) -> Unit = selectB(id { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, id { <!UNRESOLVED_REFERENCE!>it<!> }, id<(B) -> Unit> { x -> x })
    val x18: (C) -> Unit = select(id { <!DEBUG_INFO_EXPRESSION_TYPE("C")!>it<!> }, { <!DEBUG_INFO_EXPRESSION_TYPE("C")!>it<!> }, id<(B) -> Unit> { x -> x })

    // Resolution of extension/non-extension functions combination
    val x19: String.() -> Unit = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.String>")!>id { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>this<!> }<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id(fun(x: String) {})<!>)
    val x20: String.() -> Unit = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>{ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>this<!> }<!>, (fun(x: String) {}))
    val x21: String.() -> Unit = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id(fun(x: String) {})<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id(fun(x: String) {})<!>)
    select(id<String.() -> Unit>(fun(x: String) {}), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id(fun(x: String) {})<!>)
    select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function2<kotlin.String, kotlin.String, kotlin.Unit>")!>id(fun String.(x: String) {})<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function2<kotlin.String, kotlin.String, kotlin.Unit>")!>id(fun(x: String, y: String) {})<!>)
    select(id(fun String.(x: String) {}), id(fun(x: String, y: String) { }), { x: String -> <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: 'this' is not defined in this context"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing"), NO_THIS!>this<!> })
    select(id(fun String.(x: String) {}), id(fun(x: String, y: String) { }), { x -> <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: 'this' is not defined in this context"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing"), NO_THIS!>this<!> })
    select(id(fun String.(x: String) {}), id(fun(x: String, y: String) { }), { x: String, y: String -> x })
    // Convert to extension lambda is impossible because the lambda parameter types aren't specified explicitly
    select(id(fun String.(x: String) {}), id(fun(x: String, y: String) { }), { x, y -> x })
    select(<!INAPPLICABLE_CANDIDATE!>id<!>(id(fun(x: String, y: String) { }), fun String.(x: String) {}), { x, y -> x })
    val x26: Int.(String) -> Int = fun (x: String) = 10 // it must be error, see KT-38439
    // Receiver must be specified in anonymous function declaration
    val x27: Int.(String) -> Int = id(fun (x: String) = 10)
    select(id<Int.(String) -> Unit> {}, { x: Int, y: String -> x })

    // Inferring lambda parameter types by partially specified parameter types of other lambdas
    select(id { x, y -> x.<!UNRESOLVED_REFERENCE!>inv<!>() + y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, { x: Int, y -> y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, { x, y: Number -> x.inv() })
    select(id { x, y -> x.<!UNRESOLVED_REFERENCE!>inv<!>() + y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, id { x: Int, y -> y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, id { x, y: Number -> x.<!UNRESOLVED_REFERENCE!>inv<!>() })
    select({ x, y -> x.<!UNRESOLVED_REFERENCE!>inv<!>() + y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, id { x: Int, y -> y.<!UNRESOLVED_REFERENCE!>toByte<!>() }, id { x, y: Number -> x.<!UNRESOLVED_REFERENCE!>inv<!>() })

    // Inferring lambda parameter types by other specified lambda parameters; expected type is a functional type with type variables in parameter types
    takeLambdas({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> }, { x: Int -> }, { x: Nothing -> x })
    takeLambdas({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> }, { } as (Int) -> Unit, { x: Nothing -> x })
    takeLambdas({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> }, { } as (Nothing) -> Unit, { x: Int -> x })
    takeLambdas({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> }, { } as (Int) -> Unit, { } as (Nothing) -> Unit)

    // Inferring lambda parameter types by other specified lambda parameters; expected type is a functional type with type variables in parameter types; dependent type parameters
    takeLambdasWithDirectlyDependentTypeParameters({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { x: Int -> x })
    takeLambdasWithDirectlyDependentTypeParameters({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { x: Number -> x }, { x: Int -> x })
    takeLambdasWithInverselyDependentTypeParameters({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }, { x: Int -> x })
    /*
     * Interesting test case: variable can be fixed to different types randomly (`Int` or `Number`; it depends on variable fixation order)
     * if in `TypeVariableDependencyInformationProvider` `hashSet` instead of `linkedSet` for `deepTypeVariableDependencies` and `shallowTypeVariableDependencies` will be used
     */
    takeLambdasWithInverselyDependentTypeParameters({ <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>it<!> }, { x: Number -> x }, { x: Int -> x })

    // Inferring lambda parameter types by subtypes of functional type
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function3<kotlin.Nothing, kotlin.Nothing, kotlin.Nothing, kotlin.Float>")!>select(A2(), { a, b, c -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!>; <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>b<!>; <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>c<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function<kotlin.Any?>")!>select(A3(), { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction<kotlin.Any>")!>select(A3(), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>)<!>
    // Should be error as `A3::foo1` is `KFunction2`, but the remaining arguments are `KFuncion1` or `Function1`
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction<kotlin.Any?>")!>select(A3(), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>, { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!> }, { it -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>it<!> })<!>
    // It's OK because `A3::foo2` is from companion of `A3`
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Int, kotlin.Any>")!>select(A3(), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.Int, kotlin.Unit>")!>A3::foo2<!>, { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!> }, { it -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Comparable<*> & java.io.Serializable>")!>select(A4(), { x: Number -> "" })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function2<kotlin.Int, kotlin.Int, kotlin.Comparable<*> & java.io.Serializable>")!>select(A5<Int, Int>(), { x: Number, y: Int -> "" })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("A2")!>select(A2(), id { a, b, c -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!>; <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>b<!>; <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>c<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function<kotlin.Any?>")!>select(id(A3()), { <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: it"), UNRESOLVED_REFERENCE!>it<!> }, { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction<kotlin.Any>")!>select(A3(), id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>))<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction<kotlin.Any?>")!>select(A3(), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>, id { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!> }, { it -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction<kotlin.Any?>")!>select(A3(), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>, { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!> }, id { it -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction<kotlin.Any?>")!>select(id(A3()), id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>), { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!> }, { it -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>it<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction<kotlin.Any?>")!>select(id(A3()), id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>), { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!> }, id { it -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>it<!> })<!>
    // If lambdas' parameters are specified explicitly, we don't report an error, because there is proper CST – Function<Unit>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction<kotlin.Any>")!>select(id(A3()), id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>), { a: Number -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>a<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction<kotlin.Any>")!>select(id(A3()), id(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<A3, kotlin.Int, kotlin.Unit>")!>A3::foo1<!>), id { a: Number -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>a<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("A4")!>select(A4(), id { x: Number -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("A5<kotlin.Int, kotlin.Int>")!>select(id(A5<Int, Int>()), id { x: Number, y: Int -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x<!>;<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>y<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("A5<kotlin.Int, kotlin.Int>")!>select(id(A5<Int, Int>()), id { x, y -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>;<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>y<!> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function2<kotlin.Number, kotlin.Int, kotlin.Number & kotlin.Comparable<*>>")!>select(id(<!DEBUG_INFO_EXPRESSION_TYPE("A5<kotlin.Number, kotlin.Int>")!>A5()<!>), id { x: Number, y: Int -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x<!>;<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>y<!> })<!>
    val x55: Function2<Number, Int, Float> = select(id(A5()), id { x, y -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>;<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>y<!>; 1f })

    // Diffrerent lambda's parameters with proper CST
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Unit>")!>select({ x: Int -> }, { x: String -> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Unit>")!>select({ x: Int -> }, { x: Int, y: Number -> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>select(id { x: Int -> }, { x: String -> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Unit>")!>select({ x: Int -> }, id { x: Int, y: Number -> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Unit>")!>select(id { x: Int -> }, id { x: String -> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Unit>")!>select(id { x: Int -> }, id { x: Int, y: Number -> })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Comparable<*> & java.io.Serializable>")!>select({ x: Int -> 1 }, { x: String -> "" })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Number & kotlin.Comparable<*>>")!>select({ x: Int -> 1 }, { x: Int, y: Number -> 1f })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, Inv<kotlin.String>>")!>select(id { x: Int -> Inv(10) }, { x: String -> Inv("") })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function<kotlin.Any>")!>select({ x: Int -> TODO() }, id { x: Int, y: Number -> Any() })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<*, kotlin.String?>")!>select(id { x: Int -> null }, id { x: String -> "" })<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.Int>")!>select(id { x: Int -> 10 }, id { x: Int, y: Number -> TODO() })<!>
    val x68: String.(String) -> String = select(id { x: String, y: String -> "10" }, id { x: String, y: String -> "TODO()" })

    // Anonymous functions
    val x69: (C) -> Unit = selectB({ <!UNRESOLVED_REFERENCE!>it<!> }, { }, id(fun (x) { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!> }))
    select(id1(fun(it) { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!>.inv() }), id1 { x: Number -> TODO() }, id1(id2(::takeInt)))
    select(id(fun (it) { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!> }), id(id<(Int) -> Unit> { x: Number -> Unit }))
    select(id(fun (it) { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>it<!>.inv() }), id<(Int) -> Unit> { })
    val x70: (Int) -> Unit = selectNumber(id(fun (it) { }), id {}, id {})
    val x71: String.() -> Unit = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id(fun String.() { })<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.String, kotlin.Unit>")!>id(fun(x: String) {})<!>)
    val x72: String.() -> Unit = select(fun String.() { }, fun(x: String) {}) // must be error
    select(id(fun String.(x: String) {}), id(fun(x: String, y: String) { }), fun (x, y) { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>;<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>y<!> })
    select(id<Int.(String) -> Unit>(fun (x, y) {}), { x: Int, y: String -> x }) // receiver of anonymous function must be specified explicitly
    select(id<Int.(String) -> Unit>(fun Int.(y) {}), { x: Int, y: String -> x })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Nothing, kotlin.String>")!>select(A3(), fun (x) = "", { a -> <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>a<!> })<!>
}
