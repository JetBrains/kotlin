// OUT_OF_CODE_BLOCK: TRUE
// TYPE: 'A'
// SKIP_ANALYZE_CHECK

class SomeClass(val paramA: Int) {
    val someProperty: Int = param<caret>
}

// TODO: it has to be non OCB !