// !WITH_NEW_INFERENCE
open class C<T>

typealias CStar = C<*>
typealias CIn = C<in Int>
typealias COut = C<out Int>
typealias CT<T> = C<T>

class Test1 : CStar()
class Test2 : CIn()
class Test3 : COut()

class Test4 : CStar {
    constructor() : super()
}

class Test5 : CT<*>()
