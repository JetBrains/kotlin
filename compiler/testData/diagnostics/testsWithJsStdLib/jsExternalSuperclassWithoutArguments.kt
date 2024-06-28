// FIR_IDENTICAL
// ISSUE: KT-57809

package bar.baz

open external class LIcon(options: String)

external class DivIcon(options: String) : LIcon // No value passed for parameter 'options'
