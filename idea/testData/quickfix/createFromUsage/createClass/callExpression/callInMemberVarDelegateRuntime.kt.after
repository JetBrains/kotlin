// "Create class 'Foo'" "true"
// DISABLE-ERRORS

import kotlin.properties.ReadWriteProperty

open class B

class A<T>(val t: T) {
    var x: B by Foo(t, "")
}

class Foo<T>(t: T, s: String) : ReadWriteProperty<A<T>, B> {

}
