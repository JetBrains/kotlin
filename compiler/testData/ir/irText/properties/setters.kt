var foo: Int = 42

var bar: Int
    get() = 42
    set(value) = Unit // default VP name

var baz: Int
    get() = 42
    set(param) = Unit // custom VP name

var qux: Int
    get() = 42
    set(_) = Unit // VP without name

open class Foo {
    open var foo: Int = 42
}
class Bar : Foo() {
    override var foo: Int
        get() = super.foo
        set(_) {}
}
