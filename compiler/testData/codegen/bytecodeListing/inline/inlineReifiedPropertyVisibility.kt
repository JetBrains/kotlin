internal inline val <reified Z> Z.internalExtProp: String
    get() = "1"

private inline val <reified Z> Z.privateExtProp: String
    get() = "2"

class Foo {
    internal inline val <reified Z> Z.internalExtProp: String
        get() = "3"

    protected inline val <reified Z> Z.protectedExtProp: String
        get() = "4"

    private inline val <reified Z> Z.privateExtProp: String
        get() = "5"
}
