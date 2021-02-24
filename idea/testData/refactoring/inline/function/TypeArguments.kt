class FunOwner<T> {
    fun callMe0() {}
    fun callMe1(t: T) {}
    fun <X> callMe2(x: X) {}
}

fun <T, X> callFun(owner: FunOwner<T>, t: T, x: X) {
    owner.callMe0()
    owner.callMe1(t)
    owner.callMe2<caret>(x)
}