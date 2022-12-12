// SKIP_TXT

fun objectInit() {
    var x: String?
    var y: String?
    x = ""
    y = ""
    x.length // ok
    y.length // ok
    val o = object {
        init { <!SMARTCAST_IMPOSSIBLE!>x<!>.length } // ?
        init { x = null }
        init { <!SMARTCAST_IMPOSSIBLE!>x<!>.length } // bad
        init { y.length } // ok
        fun foo() = <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
    }
    y = null
    x<!UNSAFE_CALL!>.<!>length // bad
    if (<!SENSELESS_COMPARISON!>x != null<!>) x.length // ok
}

fun objectMethod() {
    var x: String?
    x = ""
    x.length // ok
    val o = object {
        init { <!SMARTCAST_IMPOSSIBLE!>x<!>.length } // sort of bad
        fun foo() = <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
        fun bar() { x = null }
        fun baz() = <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
    }
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
    if (x != null) {
        o.bar() // assign here
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
    }
}

fun classInit() {
    var x: String?
    var y: String?
    x = ""
    y = ""
    x.length // ok
    y.length // ok
    val ctor = run {
        class C {
            init { <!SMARTCAST_IMPOSSIBLE!>x<!>.length } // ?
            init { x = null }
            init { <!SMARTCAST_IMPOSSIBLE!>x<!>.length } // bad
            init { <!SMARTCAST_IMPOSSIBLE!>y<!>.length } // bad
            fun foo() = <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
        }
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
        if (x != null) {
            y = null
            C() // read y & assign x here
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
        }
        ::C
    }
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
    if (x != null) {
        ctor() // read y & assign x here
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
    }
}

fun classMethod() {
    var x: String?
    var y: String?
    x = ""
    y = ""
    x.length // ok
    y.length // ok
    val ctor = run {
        class C {
            init { <!SMARTCAST_IMPOSSIBLE!>x<!>.length } // sort of bad
            init { <!SMARTCAST_IMPOSSIBLE!>y<!>.length } // bad
            fun foo() = <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
            fun bar() { x = null }
            fun baz() = <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
        }
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
        if (x != null) {
            C().bar() // assign here
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
        }
        ::C
    }
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
    if (x != null) {
        y = null
        ctor().bar() // read y & assign x here
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
    }
}

fun runInInverseOrder(x: Any?, a: () -> Unit, b: () -> Unit) {
    b()
    a()
}

fun objectInParallelLambda() {
    var x: String?
    x = ""
    runInInverseOrder(
        object { init { x.length } }, // ok
        { object { init { <!SMARTCAST_IMPOSSIBLE!>x<!>.length } } }, // bad
        { x = null },
    )
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
}
