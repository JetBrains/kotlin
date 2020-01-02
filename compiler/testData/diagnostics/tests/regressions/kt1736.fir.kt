//KT-1736 AssertionError in CallResolver

package kt1736

object Obj {
    fun method() {
    }
}

val x = Obj.<!INAPPLICABLE_CANDIDATE!>method<!>{ -> }