// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CollectionLiterals

// FILE: file1.kt

package f1

import kotlin.reflect.KProperty

object A {
    operator fun plus(other: A) = this
    override fun equals(other: Any?) = true

    operator fun B.minus(other: B) = this
}

operator fun A.plusAssign(other: A) = Unit

class B {
    companion object {
        operator fun of(vararg xs: Int): B = B()
    }
}

operator fun B.getValue(thisRef: Any?, property: KProperty<*>): Int { return 42 }

// FILE: file2.kt

package f2

import f1.A.plus as plus
import f1.A.equals as equals
import f1.plusAssign as plusAssign
import f1.B.Companion.of as of
import f1.getValue as getValue
import f1.A.minus as minus

fun test() {
    f1.A += f1.A
    val p by f1.B()
    f1.B() - f1.B()

    plus(f1.A)
    f1.A + f1.A
    f1.A == f1.A
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, integerLiteral,
localProperty, nullableType, objectDeclaration, operator, override, propertyDeclaration, propertyDelegate,
thisExpression, vararg */
