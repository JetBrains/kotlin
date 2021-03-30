class Foo {
    <!UNNECESSARY_LATEINIT!>lateinit<!> var bar: String

    constructor(baz: Int) {
        // At best, we should have error here despite of lateinit
        bar += baz
    }
}
