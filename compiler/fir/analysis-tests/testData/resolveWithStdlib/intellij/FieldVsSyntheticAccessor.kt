// FILE: BaseClass.java

public class BaseClass {
    protected String ui = "";
}

// FILE: User.kt

package test

class User : BaseClass() {
    fun foo(tree: BaseClass) {
        val ui = tree.<!INVISIBLE_REFERENCE!>ui<!>
    }

    fun bar() {
        val ui = ui
    }

    fun baz() {
        val ui = this.ui
    }
}
