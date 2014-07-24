import kotlin.InlineOption.*

class A {
    fun foo() {
        inlineFun { "test" }
    }

    inline fun inlineFun(inlineOptions(ONLY_LOCAL_RETURN) lambda: () -> Unit) {
        val s = object {
            fun run() {
                lambda()
            }
        }
        s.run()
    }
}
