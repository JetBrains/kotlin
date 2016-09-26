class Foo {
    // Erroneous case: after lateinit removal, we'll have error at 'bar += baz', not here
    // However, looks like we must have error at 'bar += baz' even with lateinit
    // because nobody can initialize this 'bar' before constructor is called
    <!UNNECESSARY_LATEINIT!>lateinit<!> var bar: String

    constructor(baz: Int) {
        bar += baz
    }
}
