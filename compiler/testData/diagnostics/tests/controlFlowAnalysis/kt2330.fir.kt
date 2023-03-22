// IGNORE_REVERSED_RESOLVE
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
    p.<!INVISIBLE_SETTER!>x<!> = 34 //should be an error here
    p.y = 23

    fun inner() {
        p.<!INVISIBLE_SETTER!>x<!> = 44
    }
}

class R {
    val p = P();
    init {
        p.<!INVISIBLE_SETTER!>x<!> = 42
    }

    val testInGetterInOtherClass : Int
        get() {
            p.<!INVISIBLE_SETTER!>x<!> = 33
            return 3
        }
}

fun test() {
    val o = object {
        fun run() {
            <!UNRESOLVED_REFERENCE!>p<!>.x = 43
        }
    }
}
