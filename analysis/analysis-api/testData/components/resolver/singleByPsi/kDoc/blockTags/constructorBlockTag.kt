/**
 * [A<caret_1>A] - to the class, [ab<caret_2>c] - to the property
 * @constructor [A<caret_3>A] - to the class, [ab<caret_4>c] - to the property
 */
class AA(var abc: String) {

    /**
     * [A<caret_5>A] - to the class, [so<caret_6>me] - unresolved
     * @constructor [A<caret_7>A] - to the class, [so<caret_8>me] - to the property
     */
    constructor(some: Int) : this("")
}