// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
// ISSUE: KT-80485
package m2

abstract class BaseClass {
    interface NestedInterface
}

abstract class IntermediateClass : BaseClass()

val intermedicateClassUsage: Any = object : IntermediateClass() {
    inner class AnonymousInnerClass : <expr>NestedInterface</expr>
}
