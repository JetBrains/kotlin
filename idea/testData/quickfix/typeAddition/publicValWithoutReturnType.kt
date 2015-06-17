// "Specify type explicitly" "true"
package a

public fun emptyList<T>(): List<T> = null!!

public val <caret>l = emptyList<Int>()