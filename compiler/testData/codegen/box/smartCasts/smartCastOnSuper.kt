// IGNORE_BACKEND_K2: ANY

open class AbstractDocument {
    fun foo() = "OK"
}
open class ColorDocument : AbstractDocument() {
    fun bar(): String {
        return if (this is HexColorDocument) {
            super.foo()
        } else {
            "FAIL"
        }
    }
}
class HexColorDocument : ColorDocument()

fun box() = HexColorDocument().bar()