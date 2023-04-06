package test

public class TypeParametersInFlexibleTypes<A>(val javaClass: d.JavaClass<A>) {
    fun foo() = javaClass.foo()

    val bar = javaClass.bar()
}