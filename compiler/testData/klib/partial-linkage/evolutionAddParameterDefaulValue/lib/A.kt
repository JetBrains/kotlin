fun foo(param: String) = "foo before change $param"

class X(val constructorParam: String) {
    fun bar(param: String) = "bar before change $param and $constructorParam"
}

