@deprecated("", ReplaceWith("bar(p + 1)"))
public fun oldFun1(p: Int): Int = 0

@deprecated("", ReplaceWith("bar(p + 2)"))
public fun oldFun2(p: Int): Int = 0

public fun bar(p: Int): Int = p