// LANGUAGE: +ContextParameters

class Foo(i: Int) {
    context(para<caret>m: Int)
    constructor(): this(param)
}
