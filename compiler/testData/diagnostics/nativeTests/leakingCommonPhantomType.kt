// !SKIP_TXT
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -CAST_NEVER_SUCCEEDS
// WITH_RUNTIME

// FILE: commonizer_types.kt

package kotlin

public interface PlatformInt

// FILE: phantom_type_producer.kt

import kotlin.<!PHANTOM_CLASSIFIER!>PlatformInt<!>

fun <<!LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS!>T : <!PHANTOM_CLASSIFIER!>PlatformInt<!><!>> topLevelFn1(
    arg: <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!>
): <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!> {
    null!!
}

fun <!LEAKING_PHANTOM_TYPE!>topLevelFn2<!>() =
    null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>

fun <<!LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS!>T<!>> topLevelFn3(): Any where T : Number, T : <!PHANTOM_CLASSIFIER!>PlatformInt<!> {
    null!!
}

val topLevelProp1: <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!> =
    null!!

val <!LEAKING_PHANTOM_TYPE!>topLevelProp2<!> =
    null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>

interface Generic<T>
interface Stub

class Class<<!LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS!>T : <!PHANTOM_CLASSIFIER!>PlatformInt<!><!>>(
    val prop1: <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!>,
    val prop2: <!LEAKING_PHANTOM_TYPE!>List<Set<Collection<<!PHANTOM_CLASSIFIER!>PlatformInt<!>>>><!>,
    <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>val propNoTypeReference<!SYNTAX!><!> = null as <!PHANTOM_CLASSIFIER!>PlatformInt<!><!>,
) : <!LEAKING_PHANTOM_TYPE_IN_SUPERTYPES!>Stub, Generic<<!PHANTOM_CLASSIFIER!>PlatformInt<!>><!> {
    class NestedClass<<!LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS!>T: <!PHANTOM_CLASSIFIER!>PlatformInt<!><!>>(
        val ncProp: <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!>
    ) {
        fun <!LEAKING_PHANTOM_TYPE!>ncFun<!>() = null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>
    }

    inner class InnerClass<<!LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS!>T: <!PHANTOM_CLASSIFIER!>PlatformInt<!><!>>(
        val icProp: <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!>
    ) {
        fun <!LEAKING_PHANTOM_TYPE!>icFun<!>() = null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>
    }

    fun <!LEAKING_PHANTOM_TYPE!>escapeFromLocalClass<!>() = {
        class Local<T : <!PHANTOM_CLASSIFIER!>PlatformInt<!>>(
            private val prop: T,
            val lcProp: <!PHANTOM_CLASSIFIER!>PlatformInt<!>,
        ) {
            fun lcFun() = null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>
            fun escape(): T = prop
        }

        Local(null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>, null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>).escape()
    }()

    val <!LEAKING_PHANTOM_TYPE!>prop3<!> =
        null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>
    val prop4: <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!>
        get() = null!!
    var prop5: <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!> =
        null!!
    var prop6: <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!>
        get() = null!!
        set(value) { null!! }

    fun <<!LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS!>T: <!PHANTOM_CLASSIFIER!>PlatformInt<!><!>> member1(
        arg1: <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!>,
        arg2: T
    ): <!LEAKING_PHANTOM_TYPE, PHANTOM_CLASSIFIER!>PlatformInt<!> {
        null!!
    }

    fun <!LEAKING_PHANTOM_TYPE!>implicitLambdaType<!>() = {
        null as Map<Any, List<<!PHANTOM_CLASSIFIER!>PlatformInt<!>>>
    }


    /////////////// Shouldn't be reported for local usages/instances ///////////////

    fun hidden1(): Any {
        val localProp = null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>
        val fromPrivateFun = hidden3()
        return localProp
    }

    fun hidden2(): Any =
        null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>

    private fun hidden3() = null as <!PHANTOM_CLASSIFIER!>PlatformInt<!>

}
