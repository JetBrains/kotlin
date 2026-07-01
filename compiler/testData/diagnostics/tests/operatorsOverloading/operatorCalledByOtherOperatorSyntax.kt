// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

// FILE: file1.kt

package f1

import kotlin.reflect.KProperty

class B {
    companion object {
        operator fun of(vararg xs: String): B = B()
        operator fun B.unaryPlus() = this
    }
}

operator fun B.plusAssign(other: B) = Unit
operator fun B.Companion.get(vararg xs: Int): B = B()
operator fun B?.div(other: B): B = B()
operator fun B.set(thisRef: Any?, property: KProperty<*>) = Unit
operator fun B.Companion.times(other: B): B = B()

// FILE: file2.kt

package f2

import f1.<!OPERATOR_RENAMED_ON_IMPORT!>plusAssign<!> as minusAssign
import f1.<!OPERATOR_RENAMED_ON_IMPORT!>get<!> as of
import f1.<!OPERATOR_RENAMED_ON_IMPORT!>div<!> as equals
import f1.<!OPERATOR_RENAMED_ON_IMPORT!>set<!> as getValue
import f1.<!OPERATOR_RENAMED_ON_IMPORT!>times<!> as invoke
import f1.B.Companion.<!OPERATOR_RENAMED_ON_IMPORT!>unaryPlus<!> as unaryMinus


fun test() {
    val x = f1.B()
    <!OPERATOR_MODIFIER_REQUIRED!>x -= f1.B()<!>
    x.minusAssign(f1.B())
    var y: f1.B = [<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>]
    y = f1.B.of(1, 2, 3)
    y = ["1", "2", "3"]
    y = f1.B.of("1", "2", "3")
    val z: f1.B? = null
    z == f1.B()
    z.equals(f1.B())
    val t <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> f1.B()
    f1.<!OPERATOR_MODIFIER_REQUIRED!>B<!>(f1.B())
    <!OPERATOR_MODIFIER_REQUIRED!>-<!>f1.B()
    with(f1.B) {
        +f1.B()
        <!OPERATOR_MODIFIER_REQUIRED!>-<!>f1.B()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, integerLiteral,
localProperty, nullableType, objectDeclaration, operator, override, propertyDeclaration, propertyDelegate,
thisExpression, vararg */
