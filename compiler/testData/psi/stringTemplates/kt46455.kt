// ISSUE: KT-46455

class A {
    fun bar() =
        x foo(*z) ?: "${z.joinToString()}"
}
