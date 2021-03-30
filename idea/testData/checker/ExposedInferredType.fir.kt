private class A

open class B<T>(val x: T)

class C(<error descr="[EXPOSED_PARAMETER_TYPE] public function exposes its private parameter type 'A'">x: A</error>): <error descr="[EXPOSED_SUPER_CLASS] public subclass exposes its private supertype 'A'">B<A></error>(x)

private class D {
    class E
}

fun <error descr="[EXPOSED_FUNCTION_RETURN_TYPE] public function exposes its private return type 'A'">create</error>() = A()

fun <error descr="[EXPOSED_FUNCTION_RETURN_TYPE] public function exposes its private return type 'A'">create</error>(<error descr="[EXPOSED_PARAMETER_TYPE] public function exposes its private parameter type 'A'">a: A</error>) = B(a)

val <error descr="[EXPOSED_PROPERTY_TYPE] public property exposes its private type 'A'">x</error> = create()

val <error descr="[EXPOSED_PROPERTY_TYPE] public property exposes its private type 'A'">y</error> = create(x)

val <error descr="[EXPOSED_PROPERTY_TYPE] public property exposes its private type 'E'">z</error>: B<D.E>? = null
