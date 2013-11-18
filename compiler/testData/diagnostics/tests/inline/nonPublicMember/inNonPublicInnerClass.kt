// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
private class Z2 {
    private val privateProperty = 11;

    public val publicProperty:Int = 12

    private fun privateFun() {}

    public fun publicFun() {}

    private inner class ZInner {
        public inline fun test() {
            privateProperty
            privateFun()
            publicFun()
            publicProperty

            Z2().publicProperty
            Z2().publicFun()
            Z2().privateProperty
            Z2().privateFun()
        }

        inline fun testInternal() {
            privateProperty
            privateFun()
            publicFun()
            publicProperty

            Z2().publicProperty
            Z2().publicFun()
            Z2().privateProperty
            Z2().privateFun()
        }
    }
}