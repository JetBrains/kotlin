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
                { result1 = local(); }()
                result2 = local()
            }()
        }
}

//type names should be equals to same ones in localFunction.kt
fun box(): String {
    val bar = Foo().bar("OK")
    val clazz = bar::class.java
    if (result1::class.simpleName != null)
        return "fail0: ${result1::class.simpleName} != null"

    if (result2::class.simpleName != null)
        return "fail1: ${result2::class.simpleName} != null"

    //local fun class
    val enclosingClass = result1::class.java.enclosingClass
    if ("Foo\$bar$1$1$1" != enclosingClass.name)
        return "fail2: ${enclosingClass.name}"

    val enclosing2Class = enclosingClass.enclosingClass

    if ("Foo\$bar$1$1" != enclosing2Class.name)
        return "fail3: ${enclosing2Class.name}"

    val enclosing3Class = enclosing2Class.enclosingClass

    if ("Foo" != enclosing3Class.name)
        return "fail4: ${enclosing3Class.name}"

    return "O" + result1()
}
