// "Create member function 'set'" "true"
class F {
    fun get(x: X, propertyMetadata: PropertyMetadata): Int = 1
}

class X {
    var f: Int by F()<caret>
}
