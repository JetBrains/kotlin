[data(1)] class A {
    fun foo() {
        [inline] fun bar() {
            return 1
        }

        [suppress("1")] 1+1
    }
}
