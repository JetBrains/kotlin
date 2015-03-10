// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    val x: Int
    val useUnitialized = <!UNINITIALIZED_VARIABLE, UNINITIALIZED_VARIABLE!>x<!> + // reported on each secondary constructor
                         <!UNINITIALIZED_VARIABLE, UNINITIALIZED_VARIABLE!>y<!> +
                         <!UNINITIALIZED_VARIABLE, UNINITIALIZED_VARIABLE!>v<!>
    var y: Int
    val v = -1

    val useInitialized = useUnitialized + v

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val uninitialized: Int<!>

    constructor() {
        x = 1
        y = 2

        x + y + v + <!UNINITIALIZED_VARIABLE!>uninitialized<!>

        uninitialized = 3

        x + y + v + uninitialized
    }

    constructor(a: Int): super() {
        <!UNINITIALIZED_VARIABLE!>x<!> + y + v + <!UNINITIALIZED_VARIABLE!>uninitialized<!>
        x = 4
        y = 5

        x + y + v + <!UNINITIALIZED_VARIABLE!>uninitialized<!>
    }

    constructor(x: String): this() {
        x + y + v + uninitialized
    }

    //anonymous
    init {
        <!UNINITIALIZED_VARIABLE, UNINITIALIZED_VARIABLE!>y<!>
    }

    // anonymous
    init {
        y = 9
    }
}
