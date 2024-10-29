/* RootScriptStructureElement */var x: Int = 0/* DeclarationStructureElement */

if (true) {
    class LocalClass {
        fun foo() = boo
        private val boo = 9
    }

    val prop = LocalClass().foo()
    fun foo(y: Int) = y + 20
    x = foo(prop)
}

val rv = x/* DeclarationStructureElement */
