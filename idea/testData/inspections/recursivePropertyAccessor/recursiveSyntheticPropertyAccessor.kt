import a.JavaInterface

class B {
    var something: String = "123"

    class Nested : JavaInterface {
        override fun setSomething(value: String) {
            val x = something // OK
            something = value
        }

        override fun getSomething(): String {
            something = "456" // OK
            return something
        }
    }
}