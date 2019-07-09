// PROBLEM: none
package my.simple.name

class Child {
    fun f() {
        Helper<caret>.value
    }

    companion object Helper {
        val value = 1
    }
}