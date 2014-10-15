// "Create property 'foo' from usage" "true"
// ERROR: Property must be initialized or be abstract
// ERROR: Variable 'foo' must be initialized

import kotlin.properties.ReadWriteProperty

class A<T> {
    val <T> foo: ReadWriteProperty<A<T>, A<Int>>

    var x: A<Int> by foo
}
