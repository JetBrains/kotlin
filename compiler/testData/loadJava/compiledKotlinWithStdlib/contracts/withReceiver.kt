// LANGUAGE_VERSION: 1.3
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.internal.contracts.*

fun Any?.isNotNull(): Boolean {
    contract {
        returns(true) implies (this@isNotNull != null)
    }
    return this != null
}