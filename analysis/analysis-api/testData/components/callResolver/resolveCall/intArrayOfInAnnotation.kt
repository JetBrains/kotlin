annotation class RequiresPermission(val anyOf: IntArray)

@RequiresPermission(anyOf = <expr>intArrayOf(1, 2, 3)</expr>)
fun foo(): Int = 5
