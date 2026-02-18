/**
 * [A<caret_1>A] [ab<caret_2>c]
 * @constructor [A<caret_3>A] [ab<caret_4>c]
 */
class AA(var abc: String) {
    fun AA() {}
    val AA: Int = 5

    /**
     * [A<caret_5>A] [so<caret_6>me]
     * @constructor [A<caret_7>A] [so<caret_8>me]
     */
    constructor(some: Int) : this("")
}