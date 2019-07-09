// TARGET_BACKEND: JVM

// WITH_RUNTIME

package foo

import kotlin.reflect.KClass

class A<T>
class B<T, Y, U>

class TestRaw {
    val a1: A<Nothing> = A()
    val a2: A<Nothing>? = A()
    val a3: A<Nothing?> = A()
    val a4: A<Nothing?>? = A()

    var b1: B<Nothing, Int, Int> = B()
    var b2: B<String, Nothing, Int> = B()

    val l: List<Nothing> = listOf()

    fun test1(a: A<Nothing?>, b: B<Nothing, String, A<Int>>): A<Nothing>? = A()
    fun test2(a: A<Nothing?>?, b: B<Int, String, Nothing>): B<Int?, Int?, Nothing?> = B()
}

class TestNotRaw {
    val a1: A<String> = A()
    val a2: A<B<Nothing, Int, Int>>? = A()
    val a3: A<Int?> = A()
    val a4: A<Int?>? = A()

    var b1: B<Int, Int, Int> = B()
    var b2: B<String, A<String>, Int> = B()

    val l: List<String> = listOf()

    fun test1(a: A<Int?>, b: B<Int, String, A<Int>>): A<Int>? = A()
    fun test2(a: A<Int>?, b: B<Int, String, A<Nothing>>): B<Int?, Int?, Int> = B()
}

abstract class C<T> {
    abstract val foo: A<T>
    abstract fun bar(): A<T>?
}

class C1 : C<Nothing>() {
    override val foo = A<Nothing>()
    override fun bar() = foo
}
class C2 : C<String>() {
    override val foo = A<String>()
    override fun bar() = foo
}

fun testAllDeclaredMembers(klass: KClass<*>, expectedIsRaw: Boolean): String? {
    val clazz = klass.java

    for (it in clazz.declaredFields) {
        if ((it.type == it.genericType) == expectedIsRaw) return "failed on field '${clazz.simpleName}::${it.name}'"
    }

    for (m in clazz.declaredMethods) {
        for (i in m.parameterTypes.indices) {
            if ((m.parameterTypes[i] == m.genericParameterTypes[i]) == expectedIsRaw) return "failed on type of param#$i of method '${clazz.simpleName}::${m.name}'"
        }
        if (m.returnType != Void.TYPE && (m.returnType == m.genericReturnType) == expectedIsRaw) return "failed on return type of method '${clazz.simpleName}::${m.name}'"
    }

    return null
}

fun box(): String {
    testAllDeclaredMembers(TestRaw::class, expectedIsRaw = true) ?:
    testAllDeclaredMembers(TestNotRaw::class, expectedIsRaw = false)?.let { return it }

    if (C1::class.java.superclass != C1::class.java.genericSuperclass) return "failed on C1 superclass"

    if (C2::class.java.superclass == C2::class.java.genericSuperclass) return "failed on C2 superclass"

    testAllDeclaredMembers(C1::class, expectedIsRaw = true) ?:
    testAllDeclaredMembers(C2::class, expectedIsRaw = false)?.let { return it }

    return "OK"
}
