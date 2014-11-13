// "Create property 'foo'" "true"
// ERROR: Property must be initialized or be abstract
// ERROR: Variable 'foo' must be initialized

import kotlin.properties.ReadOnlyProperty

class A<T> {
    val foo: ReadOnlyProperty<A<T>, A<Int>>

    val x: A<Int> by foo
}
