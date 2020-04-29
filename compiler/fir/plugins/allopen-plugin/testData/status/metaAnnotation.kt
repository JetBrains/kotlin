import org.jetbrains.kotlin.fir.allopen.AllOpen

@Open
class A {
    fun foo() {

    }
}

@Open
class B : A() {
    override fun foo() {

    }
}

@AllOpen
annotation class Open