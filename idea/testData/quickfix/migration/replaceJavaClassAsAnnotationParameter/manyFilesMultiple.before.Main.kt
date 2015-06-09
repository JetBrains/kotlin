// "Replace Class<T> with KClass<T> for each annotation in project" "true"
// WITH_RUNTIME

annotation class Ann1(val arg: Class<*><caret>)

Ann1(javaClass<String>()) class MyClass1
Ann1(javaClass<MyClass1>()) class MyClass2

annotation class Ann2(val arg: Array<Class<*>>)

Ann2(arg = array(javaClass<Double>())) class MyClass3 @Ann1(javaClass<Char>()) constructor() {
    annotation class Ann3(val arg: Class<*> = javaClass<Any>())

    Ann3(javaClass<String>()) class Nested {
        Ann1(arg = javaClass<String>()) fun foo1() {
            annotation class LocalAnn(val arg: Class<*>)
            @LocalAnn(javaClass<Class<*>>()) val x = 1
        }
    }

    inner AnnO(javaClass<Double>()) class Inner
}

AnnO(javaClass<Boolean>()) class Another
