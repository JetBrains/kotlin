fun X.foo(param: String) = "foo before change $param"
fun bar(param: String) = "bar before change $param"

class X() {
    fun qux(param: String) = "qux before change $param"
    fun muc(param: String) = "muc before change $param"
}

