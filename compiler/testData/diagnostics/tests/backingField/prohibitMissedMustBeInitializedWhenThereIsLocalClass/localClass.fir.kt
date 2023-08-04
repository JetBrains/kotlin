fun main() {
    class Foo {
        <!MUST_BE_INITIALIZED!>var x: String<!>
            set(value) {}

        init {
            x = ""
        }
    }
}
