// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// WITH_STDLIB

val a: Int
val b = 1.also { a = 2 }
<!MUST_BE_INITIALIZED!>val c: Int<!>
val d by lazy { c = 2; 1 }
val e: Int
    get() {
        <!VAL_REASSIGNMENT!>c<!> = 3
        return c
    }

class Class {
    val i: Int
    val j = 1.also { i = 2 }
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val k: Int<!>
    val l by lazy { <!CAPTURED_MEMBER_VAL_INITIALIZATION!>k<!> = 2; 1 }
    val n: Int
        get() {
            <!VAL_REASSIGNMENT!>k<!> = 3
            return k
        }
}

fun main() {
    val x: Int
    val y = 1.also { x = 2 }
    val z: Int
    val w by lazy { <!CAPTURED_VAL_INITIALIZATION!>z<!> = 2; 1 }
}
