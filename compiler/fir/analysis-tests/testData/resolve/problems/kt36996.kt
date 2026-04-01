// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-36996
// WITH_STDLIB

// KT-36996: wrong OPERATOR_MODIFIER_REQUIRED error for Delegate class when class has valid delegate special extension functions

import kotlin.reflect.KProperty

class Delegate {
    fun getValue(thisRef: Any?, property: KProperty<*>): String { //(1)
        return ""
    }

    fun setValue(thisRef: Any?, property: KProperty<*>, value: String) { //(2)
    }
}

operator fun Delegate.getValue(thisRef: Any?, property: KProperty<*>): String {
    return ""
}
operator fun Delegate.setValue(thisRef: Any?, property: KProperty<*>, value: String) {}

fun box() {
    class Test {
        var p: String by Delegate() // OPERATOR_MODIFIER_REQUIRED for (1) and (2)

        operator fun Delegate.getValue(thisRef: Any?, property: KProperty<*>): String { // (3)
            return ""
        }
        operator fun Delegate.setValue(thisRef: Any?, property: KProperty<*>, value: String) {} //(4)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localClass, nullableType,
operator, propertyDeclaration, propertyDelegate, setter, starProjection, stringLiteral */
