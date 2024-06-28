// TARGET_BACKEND: JVM
// ALLOW_AST_ACCESS
package test

import java.util.*

public open class MethodWithTypeParameters {
    public open fun <A, B : Runnable> foo(a : A, b : List<B>, c: MutableList<in String?>) where B : List<Cloneable> {
    }
}
