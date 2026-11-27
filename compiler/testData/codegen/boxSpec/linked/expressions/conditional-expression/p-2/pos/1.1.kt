// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, conditional-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: branchless conditional expression, despite being of almost no practical use, is valid in Kotlin
 */

fun box(): String {
    if (true) else;
    if (true) else; ;
    if (true) else ;
    if (true)
    else;

    if (true) else
    ;
    val x = {
        if (true) else;
    }

    if (false) else;
    if (false) else; ;
    if (false) else ;
    if (false)
    else;

    if (false) else
    ;
    val x1 = {
        if (false) else;
    }
    return "OK"
}