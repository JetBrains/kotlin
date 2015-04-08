private fun Any.foo() {
    with("different extension receiver") {
        foo()
    }
}


class Klass(p: String) {
    private fun bar() {
        with(Klass("different dispatch receiver")) {
            bar()
        }
    }
}
