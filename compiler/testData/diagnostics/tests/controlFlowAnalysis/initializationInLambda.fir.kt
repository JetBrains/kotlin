// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun ignoreIt(f: () -> Unit) {}

fun exec(f: () -> Unit) = f()

fun foo() {
    var x: Int
    ignoreIt() {
        // Ok
        x = 42
    }
    // Error!
    x.hashCode()
}

fun bar() {
    val x: Int
    exec {
        x = 13
    }
}

fun bar2() {
    val x: Int
    fun foo() {
        x = 3
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
            xx = 42
        }
        24
    }
    else 0
}

val z = if (true) {
    val xx: Int
    exec {
        <!UNRESOLVED_REFERENCE!>xx<!> = 24
    }
    42
}
else 0