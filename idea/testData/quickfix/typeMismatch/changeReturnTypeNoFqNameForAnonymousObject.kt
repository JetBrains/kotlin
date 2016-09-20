// "Change return type of current function 'foo' to 'Int'" "true"
package foo.bar

fun test() {
    val o = object {
        fun foo(): String {
            return <caret>1
        }
    }
}