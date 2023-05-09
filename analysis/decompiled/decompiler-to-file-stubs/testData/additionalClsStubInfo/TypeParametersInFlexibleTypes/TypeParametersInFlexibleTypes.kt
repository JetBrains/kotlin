package test

public class TypeParametersInFlexibleTypes<A, D>(val javaClass: d.JavaClass<A>, val t: D & Any) {
    fun foo() = javaClass.foo()

    val bar = javaClass.bar()

    val baz = d.JavaClass.baz(t)
}