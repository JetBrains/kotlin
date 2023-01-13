//KT-897 Don't allow assignment to a property before it is defined

package kt897

class A() {
    init {
        <!INITIALIZATION_BEFORE_DECLARATION, VAL_REASSIGNMENT!>i<!> = 11
    }
    val i : Int? = null // must be an error

    init {
        <!INITIALIZATION_BEFORE_DECLARATION!>j<!> = 1
    }
    var j : Int = 2

    init {
        <!INITIALIZATION_BEFORE_DECLARATION!>k<!> = 3
    }
    val k : Int
}
