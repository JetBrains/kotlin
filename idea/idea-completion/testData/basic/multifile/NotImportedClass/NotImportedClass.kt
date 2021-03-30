// FIR_COMPARISON
package test

fun usage(): P<caret> {}

// EXIST: PublicTopLevelClass, PublicNestedClass, PublicInnerClass
// ABSENT: PrivateNestedClass, PrivateInnerClass, ProtectedNestedClass, ProtectedInnerClass, PrivateTopLevelClass