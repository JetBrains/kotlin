class A {
    companion object {
        private var r: Int = 1
            // Custom getter is needed, otherwise no need to generate getTest
            get() = field
    }
}
// A and companion object constructor call
// 3 ALOAD 0
// 1 synthetic access\$getR
