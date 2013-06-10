package test

public trait A<T> {
    fun bar(): T
    fun foo(): T = bar()
}

public class B : A<String> {
    override fun bar() = ""
}
