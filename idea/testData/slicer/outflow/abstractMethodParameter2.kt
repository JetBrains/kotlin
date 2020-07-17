// FLOW: OUT

interface I {
    fun foo(p: Any)
}

class C : I {
    override fun foo(p: Any) {
        val v = p // this usage will be shown twice due to bug in Java implementation: https://youtrack.jetbrains.com/issue/IDEA-236958
    }
}

fun bar(i: I, s: String) {
    i.foo(<caret>s)
}
