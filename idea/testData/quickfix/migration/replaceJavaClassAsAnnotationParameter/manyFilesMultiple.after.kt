// "Replace Class<T> with KClass<T> for each annotation in project" "true"
// WITH_RUNTIME

import kotlin.reflect.KClass

annotation class Ann1(val arg: KClass<*>)

Ann1(String::class) class MyClass1
Ann1(MyClass1::class) class MyClass2

annotation class Ann2(val arg: Array<KClass<*>>)

Ann2(arg = array(Double::class)) class MyClass3 [Ann1(Char::class)] () {
    annotation class Ann3(val arg: KClass<*> = Any::class)

    Ann3(String::class) class Nested {
        Ann1(arg = String::class) fun foo1() {
            annotation class LocalAnn(val arg: KClass<*>)
            [LocalAnn(Class::class)] val x = 1
        }
    }

    inner AnnO(Double::class) class Inner
}

AnnO(Boolean::class) class Another
