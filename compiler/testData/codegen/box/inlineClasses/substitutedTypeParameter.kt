// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

class Wrapper<T>(val value: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T : Any>(val x: Wrapper<T>)

object Example {
    @JvmStatic
    fun <R : Any> Foo<Int>.bar(x: Foo<R>) {}
    @JvmStatic
    fun Foo<Int>.quux(x: Foo<String>) {}
}

fun box(): String {
    val fooInt = Foo(Wrapper(1))
    val fooString = Foo(Wrapper("x"))

    with(Example) {
        fooInt.bar(fooString)
        fooInt.quux(fooString)
    }

    val wrapperClass = Wrapper::class.java
    val exampleClass = Example::class.java
    val methods = exampleClass.getDeclaredMethods()

    val barMethod = methods.single { it.name.startsWith("bar") }
    require(barMethod.toGenericString() == "public static final <R> void ${exampleClass.name}.${barMethod.name}(${wrapperClass.name}<java.lang.Integer>,${wrapperClass.name}<R>)") {
        "Unexpected method signature for 'bar': ${barMethod.toGenericString()}"
    }

    val quuxMethod = methods.single { it.name.startsWith("quux") }
    require(quuxMethod.toGenericString() == "public static final void ${exampleClass.name}.${quuxMethod.name}(${wrapperClass.name}<java.lang.Integer>,${wrapperClass.name}<java.lang.String>)") {
        "Unexpected method signature for 'quux': ${quuxMethod.toGenericString()}"
    }

    return "OK"
}