// "Create property 'foo'" "true"
// ERROR: Property must be initialized or be abstract
// ERROR: Variable 'foo' must be initialized

import kotlin.properties.ReadWriteProperty

class A<T> {
    private val foo: ReadWriteProperty<A<T>, A<Int>>
    var x: A<Int> by foo
}
