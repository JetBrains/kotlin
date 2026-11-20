// MEMBER_NAME_FILTER: I
package test

annotation class Anno(val s: String)

const val C = "c"

fun interface <caret>I<@Anno("typeParameter" + C) T> {
    @Anno("fun" + C)
    fun run(t: T): String
}
