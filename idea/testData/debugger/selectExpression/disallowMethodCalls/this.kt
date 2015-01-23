class MyClass {
    fun test() {
        <caret>this.test()
    }
}

// EXPECTED: null