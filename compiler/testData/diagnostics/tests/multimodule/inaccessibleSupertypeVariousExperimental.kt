// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78800

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

    DependencyInheritor()
    BoxedDependencyInheritor()

    dependencyInheritor.<!MISSING_DEPENDENCY_SUPERCLASS!>foo<!>()
    dependencyInheritor.<!MISSING_DEPENDENCY_SUPERCLASS!>bar<!>()

    DependencyInheritor::<!MISSING_DEPENDENCY_SUPERCLASS!>foo<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, propertyDeclaration, typeParameter */
