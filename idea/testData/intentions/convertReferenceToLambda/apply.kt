// WITH_RUNTIME

class My {
    fun foo() {}
}

val x = My().apply(<caret>My::foo)