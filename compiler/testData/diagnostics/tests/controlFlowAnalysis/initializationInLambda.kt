// IGNORE_REVERSED_RESOLVE
// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun ignoreIt(<!UNUSED_PARAMETER!>f<!>: () -> Unit) {}

fun exec(f: () -> Unit) = f()

fun foo() {
    var x: Int
    ignoreIt() {
        // Ok
        x = 42
    }
    // Error!
    <!UNINITIALIZED_VARIABLE!>x<!>.hashCode()
}

fun bar() {
    val x: Int
    exec {
        <!CAPTURED_VAL_INITIALIZATION!>x<!> = 13
    }
}

fun bar2() {
    val x: Int
    fun foo() {
        <!CAPTURED_VAL_INITIALIZATION!>x<!> = 3
    }
    foo()
}

class My(val cond: Boolean) {

    val y: Int

    init {
        val x: Int
        if (cond) {
            exec {

            }
            x = 1
        }
        else {
            x = 2
        }
        y = x
    }

    constructor(): this(false) {
        val x: Int
        x = 2
        exec {
            x.hashCode()
        }
    }
}

class Your {
    val y = if (true) {
        val xx: Int
        exec {
            <!CAPTURED_VAL_INITIALIZATION!>xx<!> = 42
        }
        24
    }
    else 0
}

val z = if (true) {
    val xx: Int
    exec {
        <!CAPTURED_VAL_INITIALIZATION!>xx<!> = 24
    }
    42
}
else 0
