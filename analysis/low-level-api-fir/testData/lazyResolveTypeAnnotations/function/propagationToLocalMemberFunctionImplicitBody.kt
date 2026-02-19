package util

@Target(AnnotationTarget.TYPE)
annotation class Anno(val str: String)

const val prop = "str"
fun bar(): @Anno("bar $prop") List<@Anno("nested bar $prop") Collection<@Anno("nested nested bar $prop") Int>>? = null
fun lambda(action: () -> Unit) = action()

fun f<caret>oo() = lambda {
    class Local {
        fun doo() = foo()
        fun foo() = bar()
        fun baz() = foo()
    }
}
