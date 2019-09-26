package inlineInClassDex.other

class TestDexInlineInClass {
    inline fun inlineFun() {
        // Breakpoint 1
        some()
        some()
    }

    fun some() {}
}
