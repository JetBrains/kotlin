// "Create function 'get' from usage" "true"
class F {
    fun set(x: X, propertyMetadata: PropertyMetadata, i: Int) { }
}

class X {
    var f: Int by F()<caret>
}
