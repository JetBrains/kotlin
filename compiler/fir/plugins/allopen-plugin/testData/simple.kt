import org.jetbrains.kotlin.fir.allopen.AllOpen

@AllOpen
class A {
    fun foo() {

    }
}

@AllOpen
class B : A() {
    override fun foo() {

    }
}