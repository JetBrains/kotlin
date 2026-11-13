// ISSUE: KT-85639
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// A dot in a property name corrupts the signature of its accessor in the KLIB:
// `Outer.member.property.<get-member.property>` is read back as `Outer.member.property.property>`.
// Inlining a function from another module before KLIB serialization mixes accessor signatures read from the KLIB
// with the ones computed for the current compilation. It still works, since both modules store the same corrupted
// signature. Only the IR deserialization test notices the corrupted signature, as in declarationsWithDotInName.kt.

// MODULE: lib
// FILE: lib.kt
package lib

class Outer {
    val `member.property`: String = "O"
    var `member.var`: String = ""
    val ` .leading property`: String = "leading property"
    val `trailing property.`: String = "trailing property"

    companion object {
        val `companion.property`: String = "companion"
    }
}

object Holder {
    val `object.property`: String = "object"
    const val `const.property`: String = "const"
}

inline fun readProperty(outer: Outer): String = outer.`member.property`

inline fun readLeadingAndTrailingProperties(outer: Outer): String = outer.` .leading property` + outer.`trailing property.`

inline fun readCompanionProperty(): String = Outer.`companion.property`

inline fun readObjectProperties(): String = Holder.`object.property` + Holder.`const.property`

inline fun writeVar(outer: Outer, value: String) {
    outer.`member.var` = value
}

// MODULE: main(lib)
// FILE: main.kt
import lib.*

fun box(): String {
    val outer = Outer()
    writeVar(outer, "K")
    if (outer.`member.var` != "K") return "FAIL: member.var"
    if (outer.` .leading property` != "leading property") return "FAIL: leading dot property"
    if (outer.`trailing property.` != "trailing property") return "FAIL: trailing dot property"
    if (readLeadingAndTrailingProperties(outer) != "leading propertytrailing property") {
        return "FAIL: leading and trailing dot properties via inline function"
    }
    if (Outer.`companion.property` != "companion") return "FAIL: companion.property"
    if (readCompanionProperty() != "companion") return "FAIL: companion.property via inline function"
    if (Holder.`object.property` != "object") return "FAIL: object.property"
    if (Holder.`const.property` != "const") return "FAIL: const.property"
    if (readObjectProperties() != "objectconst") return "FAIL: object properties via inline function"
    return readProperty(outer) + outer.`member.var`
}
