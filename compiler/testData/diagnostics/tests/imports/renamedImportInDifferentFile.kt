// FIR_IDENTICAL
// ISSUE: KT-59789
// FILE: a/A.java
package a;
public interface A {}

// FILE: first.kt
package b
import b.DependencyAnalyzerDependency as Dependency

fun foo(d: Dependency) {}

// FILE: main.kt
package b

import a.A
interface DependencyAnalyzerDependency : A {
    val parent: DependencyAnalyzerDependency? // Should be resolved
}

fun bar(d: DependencyAnalyzerDependency) {
    foo(d)
}
