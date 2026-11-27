// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, constant-literals, character-literals -> paragraph 4 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: character literal codepoint is equal to the unicode symbol codes
 */

fun box(): String {
    val a = '\u0000'
    val c = ' ' //u+0020
    val cMax = '￿' //u+ffff
    val aMax = '\uffff'
    if (a.toShort() == 0x0000.toShort()
        && c.toShort() == 0x0020.toShort()
        && cMax.toShort() == 0xffff.toShort()
        && aMax.toShort() == 0xffff.toShort())
        return "OK"
    return "NOK"
}