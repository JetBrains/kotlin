// "Replace with 'New'" "true"
package some

object New {
    fun foo() {}
}

@Deprecated("Use New", replaceWith = ReplaceWith("New"))
object Old {
    fun foo() {}
}

val test = <caret>Old.foo()