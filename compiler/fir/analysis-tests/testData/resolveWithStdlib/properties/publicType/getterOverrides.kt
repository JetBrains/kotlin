open class Base {
    protected open val foo: Int = 2
        public get(): Number
}

class Derived1 : Base() {
    public override val foo get() = 4 /* or e.g. = 2 * super.foo */
}

class Derived2 : Base() {
    // must be an error
    protected override val foo get() = 4
}

interface Fooer {
    // must be treated as a getter
    val foo: Number
}

class SimpleFooer : Fooer {
    // shouldn't be an error
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>protected<!> override val foo: Int = 2
        public get(): Number
}
