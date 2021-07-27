open class Base {
    protected open val foo: Int = 2
        public get(): Number
}

class Derived1 : Base() {
    public override val foo get() = 4 /* or e.g. = 2 * super.foo */
}

class Derived2 : Base() {
    // must be an error
    <!INCOMPLETE_PROPERTY_OVERRIDE!>protected override val foo get() = 4<!>
}

class Derived3 : Base() {
    public override val <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>foo<!> get() = "Test"
}

interface Fooer {
    // must be treated as a getter
    val foo: Number
}

class SimpleFooer : Fooer {
    // shouldn't be an error
    protected override val foo: Int = 2
        public get(): Number
}
