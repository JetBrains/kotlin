// "Specify type explicitly" "true"
package a

public fun <T> emptyList(): List<T> = null!!

public val <caret>l = emptyList<Int>()