// MODULE: topmost
// FILE: topmost.kt
package org.example

interface Base : <!CYCLIC_INHERITANCE_HIERARCHY!>TopmostAdditional<!> {
    fun topmost()
}

interface TopmostAdditional : <!CYCLIC_INHERITANCE_HIERARCHY!>Topmost<!>

interface Topmost : <!CYCLIC_INHERITANCE_HIERARCHY!>Base<!>

// MODULE: top(topmost)

// FILE: top.kt
package org.example

interface Base : TopAdditional {
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

<!MISSING_DEPENDENCY_SUPERCLASS!>interface Base : MiddleAdditional {
    fun middle()
}<!>

<!MISSING_DEPENDENCY_SUPERCLASS!>interface MiddleAdditional : Middle<!>

<!MISSING_DEPENDENCY_SUPERCLASS!>interface Middle : Top<!>

// MODULE: bottom(middle)
// FILE: bottom.kt
package org.example

<!MISSING_DEPENDENCY_SUPERCLASS!>interface Base : BottomAdditional {
    fun bottom()
}<!>

<!MISSING_DEPENDENCY_SUPERCLASS!>interface BottomAdditional : Bottom<!>

<!MISSING_DEPENDENCY_SUPERCLASS!>interface Bottom : Middle<!>
