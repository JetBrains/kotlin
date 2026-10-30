// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
// ISSUE: KT-77554
// MUTE_LL_FIR: KT-77554 the implicit type phase visits explicit setter return types

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

class A {
    var property = 1
        get() = field
        set(value): @Anno("member setter return type $prop") Unit {
            field = value
        }
}

fun foo() {
    class Local {
        fun usage() = property

        var property = 1
            get() = field
            set(value): @Anno("local setter return type $prop") Unit {
                field = value
            }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, classDeclaration, const, functionDeclaration, getter,
integerLiteral, localClass, primaryConstructor, propertyDeclaration, setter, stringLiteral */
