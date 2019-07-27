// OUT_OF_CODE_BLOCK: TRUE
// TYPE: '@Throws(Exception::class)`

class InPropertyAccessorWithAnnotation {
    val prop1: Int
        <caret>
        get() = 42
}