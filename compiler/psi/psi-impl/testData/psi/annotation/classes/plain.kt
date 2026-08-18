// The compiler forbids any member but a constructor parameter in an annotation class,
// so every property it has is one of those parameters, spelled out twice in the compiled form
package test

annotation class Empty

annotation class Single(val value: String)

annotation class Defaults(val int: Int = 1, val string: String = "s", val array: IntArray = [1, 2])

annotation class Varargs(vararg val values: String)
