// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextReceivers
// ISSUE: KT-72863

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(List<@Anno("context receiver type $prop") Int>)
class ClassWithImplicitConstructor

context(List<@Anno("context receiver type $prop") Int>)
class ClassWithExplicitConstructor() {
    constructor(i: Int) : this()
}

const val prop = "str"
