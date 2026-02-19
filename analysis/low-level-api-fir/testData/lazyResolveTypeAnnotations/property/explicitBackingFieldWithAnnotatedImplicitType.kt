@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

fun explicitType(): @Anno("return type $prop") MyList<@Anno("nested return type $prop") MyList<@Anno("nested nested return type $prop") Int>> = null!!
interface MyList<T>

open class A {
    val pr<caret>op: Any
    field = explicitType()
}
