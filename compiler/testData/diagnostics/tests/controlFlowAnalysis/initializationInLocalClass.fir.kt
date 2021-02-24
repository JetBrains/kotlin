// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun foo() {
    var x: String
    class A {
        init {
            x = ""
        }
    }
    // Error! See KT-10042
    <!UNINITIALIZED_VARIABLE!>x<!>.length
}

fun bar() {
    var x: String
    object: Any() {
        init {
            x = ""
        }
    }
    // Ok
    <!UNINITIALIZED_VARIABLE!>x<!>.length
}

fun gav() {
    val x: String
    class B {
        init {
            // Error! See KT-10445
            x = ""
        }
    }
    // Error! See KT-10042
    <!UNINITIALIZED_VARIABLE!>x<!>.length
    val y: String
    class C(val s: String) {
        constructor(): this("") {
            // Error!
            y = s
        }
    }
    <!UNINITIALIZED_VARIABLE!>y<!>.length
}

open class Gau(val s: String)

fun gau() {
    val x: String
    object: Any() {
        init {
            // Ok
            x = ""
        }
    }
    // Ok
    <!UNINITIALIZED_VARIABLE!>x<!>.length
    val y: String
    fun local() {
        object: Any() {
            init {
                // Error!
                y = ""
            }
        }
    }
    val z: String
    object: Gau(if (true) {
        z = ""
        z
    }
    else "") {}
}

class My {
    init {
        val x: String
        class Your {
            init {
                // Error! See KT-10445
                x = ""
            }
        }
    }
}

val top: Int

fun init() {
    top = 1
}
