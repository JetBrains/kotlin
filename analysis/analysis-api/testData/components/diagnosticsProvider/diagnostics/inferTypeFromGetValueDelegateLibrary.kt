// IGNORE_FE10
// KT-64503

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
class SimpleVarClass(var constructorVariable: Boolean) {
    var memberVariable: Boolean = constructorVariable
}

// MODULE: main(lib)
// FILE: usage.kt
import kotlin.reflect.KMutableProperty

fun moreFun(a: KMutableProperty<Boolean>) = a

fun testMutableProp() {
    moreFun(SimpleVarClass::constructorVariable)
    moreFun(SimpleVarClass::memberVariable)
}
