// TODO: it has to be non OCB !
// OUT_OF_CODE_BLOCK: TRUE
// ERROR: No value passed for parameter 'i2'
// TYPE: '\b\b'
class Test {
    val foo: String? = "a" + bar(1, 2<caret>)

    fun bar(i: Int, i2: Int) = ""
}

// SKIP_ANALYZE_CHECK
