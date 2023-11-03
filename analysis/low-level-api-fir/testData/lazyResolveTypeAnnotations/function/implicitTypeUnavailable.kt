package lowlevel

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

object Obj {
    private const val prop = "str"
    fun explicitType(): @Anno("return type $prop") List<@Anno("nested return type $prop") List<@Anno("nested nested return type $prop") Int>> = 0
}

fun impl<caret>icitType() = Obj.explicitType()