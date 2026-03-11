// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78800
// LANGUAGE: +AllowEagerSupertypeAccessibilityChecks

// MODULE: top
// FILE: top.kt

open class Dependency {
    fun foo() {}
}

// MODULE: middle(top)
// FILE: middle.kt

class DependencyInheritor: Dependency() {
    fun bar() {}
}

val dependencyInheritor = DependencyInheritor()

open class Box<T>
class BoxedDependencyInheritor: Box<Dependency>()

// MODULE: bottom(middle)
// FILE: bottom.kt

fun main() {
    dependencyInheritor

    <!MISSING_DEPENDENCY_SUPERCLASS!>DependencyInheritor<!>()
    BoxedDependencyInheritor()

    dependencyInheritor.<!MISSING_DEPENDENCY_SUPERCLASS, UNRESOLVED_REFERENCE!>foo<!>()
    dependencyInheritor.<!MISSING_DEPENDENCY_SUPERCLASS!>bar<!>()

    DependencyInheritor::<!MISSING_DEPENDENCY_SUPERCLASS, UNRESOLVED_REFERENCE!>foo<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, propertyDeclaration, typeParameter */
