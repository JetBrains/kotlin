// KT-66097: java.lang.IllegalStateException: JS_FAKE_NAME_CLASH: JavaScript name parse_trkh7z$ is generated for different inherited members: fun parse(source: A): C and fun parse(source: B): Collection<C> (32,17) in /returnTypeSignature.kt

// WITH_STDLIB

import kotlin.test.*

class A { }

class B { }

class C { }

interface Parser<in IN: Any, out OUT: Any> {
    fun parse(source: IN): OUT
}

interface MultiParser<in IN: Any, out OUT: Any> {
    fun parse(source: IN): Collection<OUT>
}

interface ExtendsInterface<T: Any>: Parser<A, T>, MultiParser<B, T> {
    override fun parse(source: B): Collection<T> = ArrayList<T>()
}

abstract class AbstractClass(): ExtendsInterface<C> {
    public override fun parse(source: A): C = C()
}

fun box(): String {
    val array = object : AbstractClass() { }.parse(B())

    return "OK"
}