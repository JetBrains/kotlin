class Foo constructor(p: Int) {
    constructor(p: Int) : /*rename*/this(p + 1)
}