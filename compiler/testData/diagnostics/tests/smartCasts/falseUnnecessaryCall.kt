// See KT-10276

class Bar() {
    var test: String? = null
    fun foo() {
        if (test != null) {
            // No warning: test is a mutable property
            test?.hashCode()
        }
    }
}