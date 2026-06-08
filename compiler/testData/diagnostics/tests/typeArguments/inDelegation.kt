// ISSUE: KT-84280, KT-84281, KT-84299
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
// LANGUAGE_FEATURE_TOGGLED: ForbidUselessTypeArgumentsIn25
import kotlin.reflect.*

object MyUnit

typealias UnitT = Unit
typealias MyUnitT = MyUnit

operator fun Unit.getValue(thisRef: Any?, property: KProperty<*>) = ""

class C1 {
    companion object {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = ""
    }
}

typealias C1TA1 = C1
typealias C1TA2 = C1TA1

class C2<T> {
    companion object {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = ""
    }
}

typealias C2TA1<T> = C2<T>
typealias C2TA2 = C2TA1<String>

val unit1 by Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
val unit2 by UnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
val unit3 by MyUnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

val c1_1 by C1<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
val c1_2 by C1TA1<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
val c1_3 by C1TA2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

val c2_1 by C2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
val c2_2 by C2TA1<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
val c2_3 by C2TA2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

fun test() {
    val unit1 by Unit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    val unit2 by UnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    val unit3 by MyUnitT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    val c1_1 by C1<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    val c1_2 by C1TA1<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    val c1_3 by C1TA2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    val c2_1 by C2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    val c2_2 by C2TA1<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    val c2_3 by C2TA2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
}

/* GENERATED_FIR_TAGS: callableReference, classReference, funWithExtensionReceiver, functionDeclaration, nullableType,
objectDeclaration, safeCall, typeAliasDeclaration, typeParameter */
