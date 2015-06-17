// "Specify type explicitly" "true"
package a

public fun emptyList<T>(): List<T> = null!!

<caret>public val l = emptyList<Int>()
