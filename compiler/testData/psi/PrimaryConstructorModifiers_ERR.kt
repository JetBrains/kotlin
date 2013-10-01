open class A

open class AB private {
    fun foo() {}
}

class A1 {

}

open class B<T : A> private () {

}
