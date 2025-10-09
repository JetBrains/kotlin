infix fun X.foo(param: String) = "foo before change $param"
tailrec fun bar(param: String) = "bar before change $param"

class X() {
    infix fun qux(param: String) = "qux before change $param"
    tailrec fun muc(param: String) = "muc before change $param"
}

