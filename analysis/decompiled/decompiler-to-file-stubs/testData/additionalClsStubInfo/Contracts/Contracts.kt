// JVM_FILE_NAME: ContractsKt

@file:OptIn(ExperimentalContracts::class)
package test

import kotlin.contracts.*

fun myRequire(x: Boolean) {
    contract {
        returns() implies x
    }
}

fun <R> call_InPlace(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun isNull(obj: Any?): Boolean {
    contract {
        returns(true) implies (obj != null)
    }
    return obj != null
}

fun isNotNull(foo: Any?): Any? {
    contract {
        returnsNotNull() implies (foo != null)
    }
    return foo
}

fun isString(foo: Any?): String? {
    contract {
        returnsNotNull() implies (foo is String)
    }
    return foo as? String
}

fun isNotString(foo: Any?): String? {
    contract {
        returnsNotNull() implies (foo !is String)
    }
    return if (foo is String) null else "not a string"
}

fun String?.asSafe(): String? {
    contract {
        returnsNotNull() implies (this@asSafe != null)
    }
    return this
}

fun isStringCheck(x: Any?): Any? {
    contract {
        returns(true) implies (x is Comparable<*> || x is CharSequence)
    }

    return x is String
}

fun isStringOrNumber(x: Any?): Any? {
    contract {
        returns(true) implies (x is Comparable<*> && (x is CharSequence || x is Number))
    }

    return x is String || x is Int
}

inline fun <reified T : Number> T?.test0(): Boolean {
    contract {
        returns(true) implies (this@test0 is T)
    }
    return this is T
}