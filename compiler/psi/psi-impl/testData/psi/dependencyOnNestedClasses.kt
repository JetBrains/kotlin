// FILE: DependencyOnNestedClasses.kt
package test

import dependency.*

public class DependencyOnNestedClasses : D.Nested() {
    fun f(nc: D.Companion.NestedInClassObject, i: D.Inner, ii: D.Inner.Inner, nn: D.Nested.Nested): D.Nested {
        return D.Nested()
    }
}

// FILE: dependency.kt
package dependency

class D {
    inner class Inner {
        inner class Inner
    }
    open class Nested {
        class Nested
    }

    companion object {
        class NestedInClassObject
    }
}
