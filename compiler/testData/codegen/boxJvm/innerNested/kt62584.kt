// TARGET_BACKEND: JVM_IR

package test

class C<T> {
    open inner class I1
    inner class I2 : I1()

    fun foo(): Any = I2()

    fun bar(): Any {
        open class L1
        class L2 : L1()

        return L2()
    }
}

val <T> C<T>.property: Any
    get() {
        open class L1<X>
        class L2 : L1<String>()
        return L2()
    }

fun <T> C<T>.baz(): Any {
    open class L1<X>
    class L2 : L1<String>()
    return L2()
}

fun box(): String {
    val fooSignature = C<String>().foo().javaClass.genericSuperclass.toString()
    // There is no type parameters on Android
    if (fooSignature != "test.C<T>\$I1" && fooSignature != "class test.C\$I1") return fooSignature
    val barSignature = C<String>().bar().javaClass.genericSuperclass.toString()
    if (barSignature != "class test.C\$bar\$L1") return barSignature
    val bazSignature = C<String>().baz().javaClass.genericSuperclass.toString()
    if (bazSignature != "test.Kt62584Kt\$baz\$L1<java.lang.String>" && bazSignature != "class test.Kt62584Kt\$baz\$L1") return bazSignature
    val propertySignature =  C<String>().property.javaClass.genericSuperclass.toString()
    if (propertySignature != "test.Kt62584Kt\$property\$L1<java.lang.String>" && propertySignature != "class test.Kt62584Kt\$property\$L1") return propertySignature
    return "OK"
}
