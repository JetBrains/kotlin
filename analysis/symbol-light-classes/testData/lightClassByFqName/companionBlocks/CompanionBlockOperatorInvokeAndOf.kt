// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
package one

class C(val value: Int) {
    companion {
        operator fun invoke(value: Int): C = C(value)
        fun of(value: Int): C = C(value)
    }
}
