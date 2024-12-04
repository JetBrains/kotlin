// BODY_RESOLVE
package util

@Target(AnnotationTarget.TYPE)
annotation class Anno(val str: String)

const val prop = "str"

fun f<caret>oo() {
    class Local {
        val foo get() = bar
        var bar: @Anno("bar $prop") List<@Anno("nested bar $prop") Collection<@Anno("nested nested bar $prop") Int>>? = null
        var foo2 = bar
    }
}
