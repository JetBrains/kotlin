package dexInlineInObject.other

object TestDexInlineInObject {
    inline fun inlineFun() {
        // Breakpoint 1
        some()
        some()
    }

    fun some() {}
}
