// ISSUE: KT-69975, KT-78229

/**
 *
 * [`top level`]
 * [top level]
 * [O.with space]
 * [O.`with space`]
 * @see O.with space
 * @see O.`with space`
 * [O.without_space]
 * [O.`without_space`]
 * @see O.without_space
 * @see O.`without_space`
 *
 * // Resolve incorrect code for completion
 * [O.]
 * @see O.
 */
fun main() {
}

fun `top level`() = Unit

object O {
    fun `with space`() = Unit
    fun without_space() = Unit
}