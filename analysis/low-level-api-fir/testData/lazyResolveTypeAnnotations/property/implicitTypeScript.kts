package lowlevel

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

fun explicitType(): @Anno("return type $prop") List<@Anno("nested return type $prop") List<@Anno("nested nested return type $prop") Int>> = 0

var impli<caret>citType
    get() = explicitType()
    set(value) {

    }
