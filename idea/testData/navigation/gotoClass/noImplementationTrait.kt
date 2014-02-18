package test

trait NoImplementationTrait {
    fun foo(): Int
    fun some(): String
}

// SEARCH_TEXT: NoImplemen
// REF: (test).NoImplementationTrait