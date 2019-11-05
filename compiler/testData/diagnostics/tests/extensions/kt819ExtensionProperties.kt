//KT-819 Redeclaration error for extension properties with the same name and different receivers
// FULL_JDK

import java.io.*

val InputStream.buffered : BufferedInputStream
    get() = if(this is BufferedInputStream) <!DEBUG_INFO_SMARTCAST!>this<!> else BufferedInputStream(this)

val Reader.buffered : BufferedReader
    get() = if(this is BufferedReader) <!DEBUG_INFO_SMARTCAST!>this<!> else BufferedReader(this)


//more tests
open class A() {
    open fun String.foo() {}
    open fun Int.foo() {}

    open val String.foo: Int
        get() = 0
    open val Int.foo: Int
        get() = 1
}

class B() : A() {
    override fun String.foo() {}
    override fun Int.foo() {}

    override val String.foo: Int
        get() = 0
    override val Int.foo: Int
        get() = 0

    fun use(s: String) {
        s.foo
        s.foo()
    }
}
