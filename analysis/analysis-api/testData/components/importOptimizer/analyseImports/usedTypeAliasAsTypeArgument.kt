// FILE: main.kt
package test

import dependency.FooAlias1
import dependency.FooAlias2

class ClassWithTypeParam<T>

fun functionWithTypeParam<T>() {}

fun testType(): ClassWithTypeParam<FooAlias1> {}

fun testCall() {
    functionWithTypeParam<FooAlias2>()
}

// FILE: dependency.kt
package dependency

class Foo

typealias FooAlias1 = Foo
typealias FooAlias2 = Foo