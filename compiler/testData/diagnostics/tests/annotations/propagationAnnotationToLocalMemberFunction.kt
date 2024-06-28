// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class Anno(val str: String)

const val prop = "str"
fun bar(): @Anno("bar $prop") List<@Anno("nested bar $prop") Collection<@Anno("nested nested bar $prop") Int>>? = null

fun foo() {
    class Local {
        fun doo() = foo()
        fun foo() = bar()
        fun baz() = foo()
    }
}
