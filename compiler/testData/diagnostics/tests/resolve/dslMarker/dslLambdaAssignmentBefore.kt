// LANGUAGE: -ResolveTopLevelLambdasAsSyntheticCallArgument
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80434
@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class MyDsl

fun main() {
    demo {
        scopedField = {
            touchOuterScope()
        }
    }
}


class DemoDsl {
    fun touchOuterScope() {}

    var scopedField: @MyDsl InnerScope.() -> Unit = {}
}

object InnerScope

fun demo(block: @MyDsl DemoDsl.() -> Unit) {}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, classDeclaration, functionDeclaration, functionalType,
lambdaLiteral, objectDeclaration, propertyDeclaration, typeWithExtension */
