// RUN_PIPELINE_TILL: BACKEND
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

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, const, functionDeclaration, localClass, nullableType,
primaryConstructor, propertyDeclaration, stringLiteral */
