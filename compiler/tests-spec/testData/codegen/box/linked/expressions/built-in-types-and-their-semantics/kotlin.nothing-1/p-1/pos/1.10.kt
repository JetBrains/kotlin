// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 10
 * DESCRIPTION: ckeck a common type of String and Nothing is String
 */


fun box(): String {
    var name: Any? = null
    val men = arrayListOf(Man("Phill"), Man())
    loop@ for (i in men) {
        name = i.name ?: break@loop
    }
    if (name is String?) return "OK"
    return "NOK"
}

private class Man(var name: String? = null) {}