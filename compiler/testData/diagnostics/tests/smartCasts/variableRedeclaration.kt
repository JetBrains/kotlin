// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE, -NAME_SHADOWING, -DEBUG_INFO_SMARTCAST

fun invokeLater(block: () -> Unit) = Unit

fun testLoop(b: Boolean) {
    var x: Any = ""
    x as String
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    x.length
    while (b) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        var x: Any = "hello"
        x = 1
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
    invokeLater {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun testLocalFunction() {
    var x: Any = ""
    x as String
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    x.length
    fun f() {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        var x: Any = "hello"
        x = 1
    }
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun testLocalClass() {
    var x: Any = ""
    x as String
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    x.length
    class F {
        init {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            var x: Any = "hello"
            x = 1
        }
    }
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun testAnonymousClass() {
    var x: Any = ""
    x as String
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    x.length
    val f = object {
        init {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
            var x: Any = "hello"
            x = 1
        }
    }
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}

fun testLambda() {
    var x: Any = ""
    x as String
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
    x.length
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        var x: Any = "hello"
        x = 1
    }
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    invokeLater {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}
