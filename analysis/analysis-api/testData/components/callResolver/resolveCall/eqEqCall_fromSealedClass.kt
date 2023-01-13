sealed class SealedClass {
    data class ClassOne(val data: Int) : SealedClass()
    data class ClassTwo(val value: String) : SealedClass()

    fun test(s1: SealedClass, s2: SealedClass) {
        <expr>s1 == s2</expr>
    }
}
