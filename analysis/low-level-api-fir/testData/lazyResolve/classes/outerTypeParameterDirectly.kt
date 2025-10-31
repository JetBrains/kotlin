open class X<T> {
    open inner class Y
}

class A : X<String>() {
    cla<caret>ss D<U : Y>
}
