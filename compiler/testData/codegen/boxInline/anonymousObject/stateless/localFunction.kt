// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_RUNTIME
// WITH_REFLECT
// FULL_JDK
package test

inline fun <T> myrun(block: () -> T): T {
    return block()
}

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING

import test.*

var result1 = { "fail" }
var result2 = { "fail2" }


class Foo {
    fun bar(obj: String) =
        myrun {
            {
                fun local() = { "K" }
                result1 = local();
                {
                    result2 = local()
                }()
            }()
        }
}

fun box(): String {
    val bar = Foo().bar("OK")
    val clazz = bar::class.java
    if (result1::class.simpleName != result2::class.simpleName)
        return "fail: ${result1::class.simpleName} != ${result2::class.simpleName}"

    //local fun class
    val enclosingClass = result1::class.java.enclosingClass
    if ("Foo\$bar$\$inlined\$myrun\$lambda$1$1" != enclosingClass.name)
        return "fail: ${enclosingClass.name}"

    val enclosing2Class = enclosingClass.enclosingClass

    if ("Foo\$bar$\$inlined\$myrun\$lambda$1" != enclosing2Class.name)
        return "fail: ${enclosing2Class.name}"

    val enclosing3Class = enclosing2Class.enclosingClass

    if ("Foo" != enclosing3Class.name)
        return "fail: ${enclosing3Class.name}"

    return "O" + result1()
}
