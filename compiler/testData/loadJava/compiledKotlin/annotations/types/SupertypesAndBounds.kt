//ALLOW_AST_ACCESS

package test

import java.io.Serializable

annotation class A

interface Foo<T : @A Number> : @A Serializable {
    fun <E, F : @A E> bar()
}
