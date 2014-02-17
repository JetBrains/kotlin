package a

trait T

fun T.get(thisRef: B, desc: PropertyMetadata): Int {
    return 3
}

fun T.set(thisRef: B, desc: PropertyMetadata, value: Int) {
}

class A(): T

<selection>class B {
    var a by A()
}</selection>