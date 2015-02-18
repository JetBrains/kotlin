//KT-2330 Check visibility of getters and setters correspondingly
package a

class P {
    var x : Int = 0
        private set

    var y : Int = 0

    val other = P();

    init {
        x = 23
        other.x = 4
    }

    val testInGetter : Int
       get() {
           x = 33
           return 3
       }
}

fun foo() {
    val p = P()
    <!INVISIBLE_SETTER!>p.x<!> = 34 //should be an error here
    p.y = 23

    fun inner() {
        <!INVISIBLE_SETTER!>p.x<!> = 44
    }
}

class R {
    val p = P();
    init {
        <!INVISIBLE_SETTER!>p.x<!> = 42
    }

    val testInGetterInOtherClass : Int
        get() {
            <!INVISIBLE_SETTER!>p.x<!> = 33
            return 3
        }
}

fun test() {
    val <!UNUSED_VARIABLE!>o<!> = object {
        fun run() {
            <!UNRESOLVED_REFERENCE!>p<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> = 43
        }
    }
}