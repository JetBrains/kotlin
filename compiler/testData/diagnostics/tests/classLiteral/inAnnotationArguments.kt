// WITH_REFLECT

import kotlin.reflect.KClass

annotation class Ann(val k: KClass<*>)
annotation class AnnArray(val kk: Array<KClass<*>>)

object AnObject

class C {
    companion object
}

fun foo() = "foo"

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>"foo"::class<!>)
fun test1() {}

@Ann(String::class)
fun test2() {}

@Ann(AnObject::class)
fun test4() {}

@Ann(C::class)
fun test5() {}

@Ann(C.Companion::class)
fun test6() {}

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>foo()::class<!>)
fun test7() {}

@AnnArray(arrayOf(<!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>""::class<!>, String::class, AnObject::class))
fun test8() {}

inline val <reified T> T.test9
    get() = @Ann(T::class) object {}
