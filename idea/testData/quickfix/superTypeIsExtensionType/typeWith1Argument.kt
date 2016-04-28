// "Convert supertype to '(String, String) -> Unit'" "true"

class Foo : <caret>String.(String) -> Unit {
    override fun invoke(p1: String, p2: String) {
    }
}