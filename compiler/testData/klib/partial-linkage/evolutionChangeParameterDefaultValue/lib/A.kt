fun foo(param: String = "global") = "foo before change $param"

class X(val constructorParam: String = "constructor") {
    fun bar(param: String = "member") = "bar before change $param and $constructorParam"
}

