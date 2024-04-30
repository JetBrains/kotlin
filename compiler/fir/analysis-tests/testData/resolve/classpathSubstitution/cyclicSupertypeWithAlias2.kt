// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// ^Problem with FirCompilerLazyDeclarationResolverWithPhaseChecking

// MODULE: topmost
// FILE: topmost.kt
package org.example

interface Base {
    fun topmost()
}

typealias Alias = Base

interface Topmost : Alias

// MODULE: top(topmost)

// FILE: top.kt
package org.example

interface Base : <!CYCLIC_INHERITANCE_HIERARCHY!>TopAdditional<!> {
    fun top()
}

// FILE: org/example/TopAdditional.java
package org.example;

public interface TopAdditional extends Top {}

// FILE: org/example/Top.java
package org.example;

public interface Top extends Topmost {}

// MODULE: middle(top)
// FILE: middle.kt
package org.example

interface Base : <!CYCLIC_INHERITANCE_HIERARCHY!>MiddleAdditional<!> {
    fun middle()
}

interface MiddleAdditional : <!CYCLIC_INHERITANCE_HIERARCHY!>Middle<!>

interface Middle : <!CYCLIC_INHERITANCE_HIERARCHY!>Top<!>

// MODULE: bottom(middle)
// FILE: bottom.kt
package org.example

interface Base : BottomAdditional {
    fun bottom()
}

interface BottomAdditional : Bottom

interface Bottom : Middle
