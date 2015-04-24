//KT-1736 AssertionError in CallResolver

package kt1736

object Obj {
    fun method() {
    }
}

val x = Obj.method<!TOO_MANY_ARGUMENTS!>{ -> }<!>