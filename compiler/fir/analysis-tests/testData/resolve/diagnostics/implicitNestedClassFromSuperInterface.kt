// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80485
package m2

interface BaseInterface {
    interface NestedInterface
}

abstract class IntermediateClass : BaseInterface
interface IntermediateInterface : BaseInterface

val intermedicateClassUsage: Any = object : IntermediateClass() {
    inner class AnonymousInnerClass : <!UNRESOLVED_REFERENCE!>NestedInterface<!>
}

val intermedicateInterfaceUsage: Any = object : IntermediateInterface {
    inner class AnonymousInnerClass : <!UNRESOLVED_REFERENCE!>NestedInterface<!>
}

val directInterfaceUsage: Any = object : BaseInterface {
    inner class AnonymousInnerClass : <!UNRESOLVED_REFERENCE!>NestedInterface<!>
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, inner, interfaceDeclaration, localClass, nestedClass,
propertyDeclaration */
