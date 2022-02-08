// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FILE: A.java

public class A {
    @Annos(value = @Anno(token = "OK"))
    @Strings(value = "OK")
    @Ints(value = 42)
    @Enums(value = E.EA)
    @Classes(value = double.class)
    public void test() {}
}

// FILE: C.kt

import kotlin.reflect.KClass

annotation class Anno(val token: String)
enum class E { EA }

annotation class Annos(val value: Array<Anno>)
annotation class Strings(val value: Array<String>)
annotation class Ints(val value: IntArray)
annotation class Enums(val value: Array<E>)
annotation class Classes(val value: Array<KClass<*>>)

class C : A()
