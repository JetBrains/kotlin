fun test() {
    while (true) {
        class LocalClass(val x: Int) {
            init {
                break
            }
            constructor() : this(42) {
                break
            }
            fun foo() {
                break
            }
        }
    }
}
