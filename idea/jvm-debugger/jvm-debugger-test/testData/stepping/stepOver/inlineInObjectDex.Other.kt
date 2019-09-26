package inlineInObjectDex.other

object TestDexInlineInObject {
    inline fun inlineFun() {
        // Breakpoint 1
        some()
        some()
    }

    fun some() {}
}
