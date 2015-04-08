// NO_ERRORS_DUMP
package a

trait T

fun T.get(thisRef: B, desc: PropertyMetadata): Int {
    return 3
}

fun T.set(thisRef: B, desc: PropertyMetadata, value: Int) {
}

class A(): T

<selection>class B {
    var v by A()
}</selection>