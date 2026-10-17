// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT
// MODULE: lib
// FILE: lib.kt
package lib

// Compiled without Valhalla value classes, so it is an identity class in its class file.
abstract value class IdentityBase {
    abstract val x: Int
    fun twice(): Int = x * 2
}

// MODULE: main(lib)
// VALHALLA_VALUE_CLASSES
// FILE: main.kt
import lib.IdentityBase

value class FromIdentity(override val x: Int) : IdentityBase()

abstract value class Middle : IdentityBase()

value class FromMiddle(override val x: Int) : Middle()

value class Independent(val x: Int)

// Value classes compiled as identity classes are listed anyway, as they become value classes once their superclasses are recompiled.
class Holder(val fromIdentity: FromIdentity, val fromMiddle: FromMiddle, val independent: Independent)

fun box(): String {
    if (FromIdentity(1).javaClass.isValue) return "FromIdentity is a value class"
    if (Middle::class.java.isValue) return "Middle is a value class"
    if (FromMiddle(2).javaClass.isValue) return "FromMiddle is a value class"
    if (!Independent(3).javaClass.isValue) return "Independent is not a value class"
    if (FromIdentity(21).twice() != 42 || FromMiddle(4).twice() != 8) return "twice"
    val holder = Holder(FromIdentity(5), FromMiddle(6), Independent(7))
    if (holder.fromIdentity.x + holder.fromMiddle.x + holder.independent.x != 18) return "Holder"
    if (FromIdentity(1) != FromIdentity(1) || FromIdentity(1) == FromIdentity(2)) return "equals"
    return "OK"
}

// 1 ATTRIBUTE LoadableDescriptors
// 1 ATTRIBUTE LoadableDescriptors : LFromIdentity;, LFromMiddle;, LIndependent;\n
