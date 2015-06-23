// "Create member function 'get'" "true"
class F {
    fun set(x: X, propertyMetadata: PropertyMetadata, i: Int) { }
}

class X {
    var f: Int by F()<caret>
}
