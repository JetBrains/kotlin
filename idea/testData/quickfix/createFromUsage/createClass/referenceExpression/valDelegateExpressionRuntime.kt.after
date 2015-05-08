// "Create object 'Foo'" "true"
// DISABLE-ERRORS

import kotlin.properties.ReadOnlyProperty

open class B

class A {
    val x: B by Foo
}

object Foo : ReadOnlyProperty<A, B> {

}
