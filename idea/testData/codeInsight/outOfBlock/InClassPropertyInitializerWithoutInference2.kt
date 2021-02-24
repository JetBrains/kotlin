// TODO: it has to be non OCB !
// OUT_OF_CODE_BLOCK: TRUE
// ERROR: The integer literal does not conform to the expected type String?
// TYPE: '\b\b\b\b'
class Test {
    val foo: String? = "a"+<caret>1

    fun bar(i: Int, i2: Int) = ""
}

// SKIP_ANALYZE_CHECK
