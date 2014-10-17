// "Create function 'get' from usage" "true"
class F {
    fun set(x: X, propertyMetadata: PropertyMetadata, i: Int) { }

    fun get(x: X, propertyMetadata: PropertyMetadata): Int {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class X {
    var f: Int by F()
}
