// ISSUE: KT-58719

// MODULE: a
// FILE: pagind/QueryPagingSource.kt

package pagind

internal class QueryPagingSource<Key : Any, RowType : Any>

fun <RowType : Any> QueryPagingSource(randomParam: Int) {}

// MODULE: b(a)
// FILE: Main.kt

package main

import pagind.QueryPagingSource

fun test() {
    QueryPagingSource<Int>(10)
    <!INVISIBLE_REFERENCE!>QueryPagingSource<!><Int, String>()
}
