// TARGET_BACKEND: JVM
// FILE: J.java

import java.util.List;

// This is a bit more elaborate version of overrideWithArrayParameterType.kt, which also checks interaction of:
// flexible nullability, flexible mutability, raw types, varargs.
interface J {
    List<String>[] foo(List... a);
}

// FILE: 1.kt

class C1 : J {
    override fun foo(vararg a: MutableList<Any?>?): Array<out MutableList<String>>? = null
}
class C2 : J {
    override fun foo(vararg a: List<Any?>): Array<MutableList<out String>>? = null
}
class C3 : J {
    override fun foo(vararg a: List<*>): Array<List<String>?>? = null
}
class C4 : J {
    override fun foo(vararg a: MutableList<out Any?>): Array<out List<String?>>? = null
}
class C5 : J {
    override fun foo(a: Array<List<Any?>>): Array<MutableList<String>>? = null
}
class C6 : J {
    override fun foo(a: Array<out MutableList<Any?>?>): Array<out MutableList<String>>? = null
}

fun box(): String {
    C1().foo()
    C2().foo()
    C3().foo()
    C4().foo()
    C5().foo(emptyArray())
    C6().foo(emptyArray())
    return "OK"
}
