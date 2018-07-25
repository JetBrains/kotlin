// !LANGUAGE: +AllowContractsForCustomFunctions +ReadDeserializedContracts
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.internal.contracts.*

class A

fun deeplyNested(x: Any?, y: Any?, b: Boolean, s: String?) {
    contract {
        returns(true) implies (((x is Int && x is String) || (x is Int && y is A) || b || (!b)) && s != null && (y is A || x is String))
    }
}