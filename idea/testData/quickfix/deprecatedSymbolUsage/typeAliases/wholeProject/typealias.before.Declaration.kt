package dependency.d

class A<T>

@Deprecated("", ReplaceWith("A<Int>", "dependency.d.A"))
typealias OldAlias = A<Int>


val usage: OldAlias? = null