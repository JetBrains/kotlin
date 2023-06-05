// TARGET_BACKEND: JVM
// WITH_REFLECT
package test

import kotlin.reflect.KClass
import kotlin.test.assertEquals

annotation class Foo(val value: String)

annotation class Anno(
        val level: DeprecationLevel,
        val klass: KClass<*>,
        val foo: Foo,
        val levels: Array<DeprecationLevel>,
        val klasses: Array<KClass<*>>,
        val foos: Array<Foo>
)

@Anno(
        DeprecationLevel.WARNING,
        Number::class,
        Foo("OK"),
        arrayOf(DeprecationLevel.WARNING),
        arrayOf(Number::class),
        arrayOf(Foo("OK"))
)
fun foo() {}

fun box(): String {
    // Construct an annotation with exactly the same parameters, check that the proxy created by Kotlin and by Java reflection are the same and have the same hash code
    val a1 = Anno::class.constructors.single().call(
            DeprecationLevel.WARNING,
            Number::class,
            Foo::class.constructors.single().call("OK"),
            arrayOf(DeprecationLevel.WARNING),
            arrayOf(Number::class),
            arrayOf(Foo::class.constructors.single().call("OK"))
    )
    val a2 = ::foo.annotations.single() as Anno

    assertEquals(a1, a2)
    assertEquals(a2, a1)
    assertEquals(a1.hashCode(), a2.hashCode())

    assertEquals("@test.Anno(level=WARNING, klass=class java.lang.Number, foo=@test.Foo(value=OK), " +
                 "levels=[WARNING], klasses=[class java.lang.Number], foos=[@test.Foo(value=OK)])", a1.toString())

    return "OK"
}
