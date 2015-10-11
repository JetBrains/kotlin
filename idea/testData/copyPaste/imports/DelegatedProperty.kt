// NO_ERRORS_DUMP
package a

interface T

fun T.getValue(thisRef: B, desc: PropertyMetadata): Int {
    return 3
}

fun T.setValue(thisRef: B, desc: PropertyMetadata, value: Int) {
}

class A(): T

<selection>class B {
    var v by A()
}</selection>