package one.two.three

/**
 * [<caret_1>one]
 *
 * [<caret_2>one.two]
 * [one.<caret_3>two]
 *
 * [<caret_4>one.two.three]
 * [one.<caret_5>two.three]
 * [one.two.<caret_6>three]
 *
 * [<caret_7>one.two.three.Four.Five]
 * [one.<caret_8>two.three.Four.Five]
 * [one.two.<caret_9>three.Four.Five]
 */
fun usage() {}

class Four {
    class Five
}