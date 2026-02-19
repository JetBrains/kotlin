/**
 * [`With Space`.`with<caret_1> space`]
 * @see `With Space`.`with<caret_2> space`
 * [`<caret_3>`] // Check a corner-case
 * @see `With Space`.`<caret_4>`
 * [O.`with space<caret_5>`]
 * @see O.`with space<caret_6>`
 */
fun main() {
}

object `With Space` {
    fun `with space`() = Unit
}

object O {
    fun `with space`() {}
}