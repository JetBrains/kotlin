// FIR_IDENTICAL
// ISSUE: KT-60154

// MODULE: lib
// FILE: lib.kt

interface Column<TData : Any, TValue>

fun <T : Any> column(): Column<T, Void> = TODO()

fun <T : Any> column(id: String): Column<T, String> = TODO()

typealias Void = Nothing?

// MODULE: app(lib)
// FILE: app.kt

interface RowData<T : Comparable<T>>

typealias ReadonlyArray<T> = Array<out T>

private val ALL_COLUMNS: ReadonlyArray<Column<RowData<*>, *>> = arrayOf(column(id = ""), column())
