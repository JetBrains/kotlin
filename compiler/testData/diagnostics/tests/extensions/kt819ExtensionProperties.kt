
//KT-819 Redeclaration error for extension properties with the same name and different receivers

import java.io.*

inline val InputStream.buffered : BufferedInputStream
    get() = if(this is BufferedInputStream) this else BufferedInputStream(this)

inline val Reader.buffered : BufferedReader
    get() = if(this is BufferedReader) this else BufferedReader(this)


//more tests
open class A() {
    open fun String.foo() {}
    open fun Int.foo() {}

    open val String.foo = 0
    open val Int.foo = 1
}

class B() : A() {
    override fun String.foo() {}
    override fun Int.foo() {}

    override val String.foo = 0
    override val Int.foo = 0

    fun use(s: String) {
        s.foo
        s.foo()
    }
}
