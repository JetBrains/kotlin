class OuterClass {
    class InnerClass {
        inline fun calculate(): Int {
            val impl = { 42 }
            return impl()
        }
    }
}
