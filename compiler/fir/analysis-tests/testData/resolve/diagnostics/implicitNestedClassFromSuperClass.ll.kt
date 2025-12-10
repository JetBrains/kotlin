// LL_FIR_DIVERGENCE
// KT-80485
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80485
package m2

abstract class BaseClass {
    interface NestedInterface
}

abstract class IntermediateClass : BaseClass()

val intermedicateClassUsage: Any = object : IntermediateClass() {
    inner class AnonymousInnerClass : <!UNRESOLVED_REFERENCE!>NestedInterface<!>
}

val directClassUsage: Any = object : BaseClass() {
    inner class AnonymousInnerClass : NestedInterface
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, inner, interfaceDeclaration, localClass, nestedClass,
propertyDeclaration */
