// "Create member function 'getValue'" "true"
class F {
    fun setValue(x: X, propertyMetadata: PropertyMetadata, i: Int) { }
}

class X {
    var f: Int by F()<caret>
}
