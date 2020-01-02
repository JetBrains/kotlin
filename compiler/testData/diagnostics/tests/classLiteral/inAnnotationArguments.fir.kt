// !LANGUAGE: +ProhibitTypeParametersInClassLiteralsInAnnotationArguments

import kotlin.reflect.KClass

annotation class Ann(val k: KClass<*>)
annotation class AnnArray(val kk: Array<KClass<*>>)

object AnObject

class C {
    companion object
}

fun foo() = "foo"

@Ann("foo"::class)
fun test1() {}

@Ann(String::class)
fun test2() {}

@Ann(AnObject::class)
fun test4() {}

@Ann(C::class)
fun test5() {}

@Ann(C.Companion::class)
fun test6() {}

@Ann(foo()::class)
fun test7() {}

@AnnArray(arrayOf(""::class, String::class, AnObject::class))
fun test8() {}

inline val <reified T> T.test9
    get() = @AnnArray(arrayOf(
        T::class,
        Array<T>::class,
        Array<Array<Array<T>>>::class
    )) object {}

inline val <reified T> T.test10
    get() = @AnnArray([T::class]) object {}
