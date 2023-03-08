// FIR_IDENTICAL

typealias Aliased = String
annotation class Tag(val tags: Aliased) // K1: ok, K2: INVALID_TYPE_OF_ANNOTATION_MEMBER
