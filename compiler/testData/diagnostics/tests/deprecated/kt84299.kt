// RUN_PIPELINE_TILL: BACKEND

import kotlin.reflect.KProperty

class X(x: String) {
    @Deprecated("", level = DeprecationLevel.ERROR)
    companion object {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = ""
    }
}

fun test() {
    val delegated by X
}

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

    fun test() {
        val delegated by Y
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, nestedClass, nullableType,
objectDeclaration, operator, primaryConstructor, propertyDeclaration, propertyDelegate, starProjection, stringLiteral */
