/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 1
 * DESCRIPTION: Type annotations on return type with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

@Target(AnnotationTarget.TYPE)
annotation class Ann

fun foo(x: String): @Ann(unresolved_reference) String {  // OK, error only in IDE but not in the compiler
    return x
}

fun box(): String? {
    return "OK"
}
