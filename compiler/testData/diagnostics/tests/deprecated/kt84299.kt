// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-84299

import kotlin.reflect.KProperty

@RequiresOptIn
annotation class A

class X {
    fun baz(x: CharSequence): Int = 1

    @A
    @Deprecated("", level = DeprecationLevel.ERROR)
    companion object {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = ""
        operator fun plusAssign(x: String) {}

        fun baz(x: String): Int = 1
    }
}

val delegated by X

fun <T> foo(x: (X, T) -> Int) = 1
fun foo(x: (String) -> Int) = ""

fun testCallableReferences() {
    val delegated by X
    X += ""
    val x: String = foo(X::baz)
}

// Correct behavior before KT-84299
object Y {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = ""
}

class HiddenCase {
    class Y {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object {
            operator fun getValue(thisRef: Any?, property: KProperty<*>) = ""
        }
    }

    val delegated by Y

    fun test() {
        val delegated by Y
    }
}

@A
class Z2 {
    companion object {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = ""
        operator fun plusAssign(x: String) {}
    }
}

val delegatedZ2 by <!OPT_IN_USAGE_ERROR!>Z2<!>

fun testZ2(){
    val delegatedZ2 by <!OPT_IN_USAGE_ERROR!>Z2<!>
    <!OPT_IN_USAGE_ERROR!>Z2<!> += ""
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, nestedClass, nullableType,
objectDeclaration, operator, primaryConstructor, propertyDeclaration, propertyDelegate, starProjection, stringLiteral */
