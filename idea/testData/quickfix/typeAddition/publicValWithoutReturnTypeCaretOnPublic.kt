// "Specify type explicitly" "true"
package a

public fun <T> emptyList(): List<T> = null!!

<caret>public val l = emptyList<Int>()
