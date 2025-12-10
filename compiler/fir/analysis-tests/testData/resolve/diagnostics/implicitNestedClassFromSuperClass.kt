// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80485
package m2

abstract class BaseClass {
    interface NestedInterface
}

abstract class IntermediateClass : BaseClass()

val intermedicateClassUsage: Any = object : IntermediateClass() {
    inner class AnonymousInnerClass : NestedInterface
}

val directClassUsage: Any = object : BaseClass() {
    inner class AnonymousInnerClass : NestedInterface
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, inner, interfaceDeclaration, localClass, nestedClass,
propertyDeclaration */
