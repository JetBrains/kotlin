// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED

class extensionInClassReference {
    fun Int.foo() {}

    val bar = Int::foo
}
