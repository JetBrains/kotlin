// LANGUAGE: -ContractSyntaxV2
// WITH_STDLIB

import kotlin.contracts.*

inline fun <reified T> requreIsInstance(value: Any) contract <!UNSUPPORTED_FEATURE!>[
    returns() implies (value is T)
]<!> {
    if (value !is T) throw IllegalArgumentException()
}

val Any?.myLength: Int?
    get() contract <!UNSUPPORTED_FEATURE!>[
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsNotNull() implies (this@length is String)<!>
    ]<!> = (this as? String)?.length

fun test_1(x: Any) {
    requreIsInstance<String>(x)
    x.length
}

fun test_2(x: Any) {
    if (x.myLength != null) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
