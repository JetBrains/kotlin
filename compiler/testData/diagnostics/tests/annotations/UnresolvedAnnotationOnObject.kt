package test

// Checks that there is no rewrite error at ANNOTATION slice because of resolving annotations for object in lazy resolve and resolving
// object as property (method tries to resolve annotations too).

@<!UNRESOLVED_REFERENCE!>BadAnnotation<!>
object SomeObject

val some = SomeObject