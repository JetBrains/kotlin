// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FIR_DUMP
package second

@Target(AnnotationTarget.TYPE)
annotation class Anno(val int: Int)

interface Base
fun bar(): Base = object : Base {}

const val constant = 0

class MyClass: @Anno(constant) Base by bar() {
    @Target(AnnotationTarget.TYPE)
    annotation class Anno(val string: String)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousObjectExpression, classDeclaration, const, functionDeclaration,
inheritanceDelegation, integerLiteral, interfaceDeclaration, nestedClass, primaryConstructor, propertyDeclaration */
