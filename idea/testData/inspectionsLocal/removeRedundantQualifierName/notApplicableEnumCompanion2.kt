// PROBLEM: none
// WITH_RUNTIME
package com.apurebase.test

import com.apurebase.test.QueryOperation.*

enum class QueryOperation(val id: Int) {
    Equals(1),
    Like(2);

    companion object O {
        fun from(name: String) = values().firstOrNull { it.name == name }
    }
}

fun test(value1: String?) {
    value1?.let(<caret>QueryOperation.O::from)
}