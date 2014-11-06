// "Create object 'Foo'" "true"
// DISABLE-ERRORS

import kotlin.properties.ReadWriteProperty

open class B

class A {
    var x: B by Foo
}

object Foo : ReadWriteProperty<A, B> {

}
