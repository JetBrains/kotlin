actual interface A
actual interface B {
    fun z()
    fun x()
}

<error descr="[ABSTRACT_MEMBER_NOT_IMPLEMENTED] Class 'Impl' is not abstract and does not implement abstract member public abstract fun x(): Unit defined in Both">class Impl</error> : Both {
    override fun z() {
    }
}