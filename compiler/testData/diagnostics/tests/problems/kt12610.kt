// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-12610
// WITH_STDLIB

// KT-12610: "RuntimeException: No DELEGATED_PROPERTY_RESOLVED_CALL" on using implicit invoke with delegated properties
import kotlin.reflect.KProperty

class Test {
    val Z.getValue: (t: Any?, p: KProperty<*>) -> Int get() = { t, p -> 1 }

    val prop: Int by <!NOT_FUNCTION_AS_OPERATOR!>Z()<!>

    fun box(): String {
        print(prop)
        return "" + prop
    }
}

class Z

fun main(args: Array<String>) {
    Test().box()
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, functionalType, getter, integerLiteral,
lambdaLiteral, nullableType, propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, starProjection,
stringLiteral */
