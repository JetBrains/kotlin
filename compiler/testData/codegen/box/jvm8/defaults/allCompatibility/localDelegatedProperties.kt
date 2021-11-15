// CHECK_BYTECODE_LISTING
// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// WITH_REFLECT
import kotlin.reflect.KProperty


class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): String {
        return p.returnType.toString()
    }
}

interface Foo {

    fun test(): String {
        val OK by Delegate()
        return OK
    }
}

fun box(): String {
    return if (object : Foo {}.test() != "kotlin.String") "fail" else "OK"
}