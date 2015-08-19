object Object {
    class NestedClass {
        fun test() {
            outerFun()
            outerVal
            OuterObject
            OuterClass()
        }
    }

    fun outerFun() {}
    val outerVal = 4

    object OuterObject
    class OuterClass
}
