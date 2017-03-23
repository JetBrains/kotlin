//ALLOW_AST_ACCESS

package test

annotation class A

interface Foo<T : @A Number> : @A CharSequence {
    fun <E, F : @A E> bar()
}
