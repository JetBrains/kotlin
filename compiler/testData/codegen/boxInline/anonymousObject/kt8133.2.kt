package test

public inline fun <T, R> T.myLet(f: (T) -> R): R = f(this)
