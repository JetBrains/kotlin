// WITH_RUNTIME

@file:[JvmName("Foo") JvmMultifileClass]
package test

fun foo() {
    "".extProp
}

inline val <reified Z> Z.extProp: String
    get() = "123"
