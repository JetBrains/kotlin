// ISSUE: KT-68556

class Clazz() {
    <!PROPERTY_WITH_NO_TYPE_NO_INITIALIZER!>var foo<!>
    <!PROPERTY_WITH_NO_TYPE_NO_INITIALIZER!>var bar<!>
        get() {
            return 0
        }

    init {
        foo = "hello"
        bar = 0
    }
}
