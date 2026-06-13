// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82964
// LATEST_LV_DIFFERENCE

import kotlin.reflect.KClass

@Repeatable
annotation class Foo<T>(val arr: Array<Bar<T>>)
annotation class Bar<T>

@Foo<Int>(<!ARGUMENT_TYPE_MISMATCH!>[Bar()]<!>)
@Foo<Int>([Bar<Int>()])
fun test() = Unit

annotation class Foo2<T : Any>(vararg val kClass: KClass<out T>)
annotation class Bar2<T : Any>(val arr: Array<Foo2<T>>)
annotation class Baz2<T : Any>(val arr: Foo2<T>)

@Bar2<Any>(<!ARGUMENT_TYPE_MISMATCH!>[Foo2(String::class)]<!>)
@Baz2<Any>(Foo2(String::class))
fun test2() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, nullableType, primaryConstructor,
propertyDeclaration, typeParameter */
