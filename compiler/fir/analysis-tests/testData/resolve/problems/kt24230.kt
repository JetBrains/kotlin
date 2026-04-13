// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-24230
// WITH_STDLIB

// KT-24230: overload resolution wrong for primitive types and function reference without argument

import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

class B { fun arg0() = 1; fun arg1(i: Int) = "a"; fun id(i: Int) = i }
class A<T: Any>(val t: T)

fun <T: Any, R: Any> A<T>.foo(f: KFunction1<T, R>, a: A<R>.() -> Unit): A<R> = A(f(t))
fun <T: Any, T1, R: Any> A<T>.foo(f: KFunction1<T1, R>, arg1: T1): A<R> = A(f(arg1))

fun <T: Any, T1, R: Any> A<T>.bar(f: KFunction2<T, T1, R>, arg1: T1, a: A<R>.() -> Unit): A<R> = A(f(t, arg1))
fun <T: Any, T1, T2, R: Any> A<T>.bar(f: KFunction2<T1, T2, R>, arg1: T1, arg2: T2): A<R> = A(f(arg1, arg2))

fun <R: Any> l(a: A<R>.() -> Unit) = a

fun test() {
    A(B()).apply {
        foo(B::arg0) { println(t) } // should call foo1 but fails overload resolution
        val v: A<Int>.() -> Unit = { println(t) }
        foo(B::arg0, v) // calls foo1, works due to the extra hint
        foo(B::arg0, l({ println(t) })) // calls foo1, works due to the extra hint
        foo(t::arg1, 1) // calls foo2

        bar(B::arg1, 1) {} // calls bar1, works
        bar(B::id, 1) {} // calls bar1, works
    }
    A(1).apply {
        bar(Int::and, 1) {} // calls bar1, works
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, integerLiteral, lambdaLiteral, localProperty, primaryConstructor, propertyDeclaration, stringLiteral,
typeConstraint, typeParameter, typeWithExtension */
