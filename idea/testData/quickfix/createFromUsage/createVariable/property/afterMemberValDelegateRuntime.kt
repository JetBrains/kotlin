// "Create property 'foo' from usage" "true"
// ERROR: Property must be initialized or be abstract
// ERROR: Variable 'foo' must be initialized

import kotlin.properties.ReadOnlyProperty

class A<T> {
    val <T> foo: ReadOnlyProperty<A<T>, A<Int>>

    val x: A<Int> by foo
}
