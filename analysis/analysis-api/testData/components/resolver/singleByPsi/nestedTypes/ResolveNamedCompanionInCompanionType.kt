package foo.bar.baz

class X {
    companion object COMP {
        fun lol() {}
    }
}

class E {
    val x = foo.bar.baz.X.CO<caret>MP
}