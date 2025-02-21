// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +AllowCheckForErasedTypesInContracts

import kotlin.contracts.*
import kotlin.reflect.KClass

fun <T : Any> checkInstanceOf(kClass: KClass<T>, value: Any?): Boolean {
    contract {
        returns(true) implies (value is T)
    }
    return kClass.isInstance(value)
}

fun testInstanceOf(x: Any) {
    if (checkInstanceOf(String::class, x)) {
        // smartcast to String
        x.length
    }
}

fun List<Any?>.isStringList(): Boolean {
    contract {
        returns(true) implies (this@isStringList is List<String>)
    }
    return this.all { it is String }
}

fun testInstanceOf(list: List<Any?>) {
    if (list.isStringList()) {
        list.map { it.length }
    }
}
