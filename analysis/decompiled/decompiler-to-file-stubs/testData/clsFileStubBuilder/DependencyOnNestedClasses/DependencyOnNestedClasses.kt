// FIR_IDENTICAL
package test

import dependency.*

public class DependencyOnNestedClasses : D.Nested() {
    fun f(nc: D.Companion.NestedInClassObject, i: D.Inner, ii: D.Inner.Inner, nn: D.Nested.Nested): D.Nested {
        return D.Nested()
    }
}
