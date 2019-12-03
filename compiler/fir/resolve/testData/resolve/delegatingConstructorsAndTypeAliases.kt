open class Inv<T, R>(x: T, r: R)

typealias Alias<X> = Inv<X, Inv<X, X>>

class InvImpl : Alias<String>("", Inv("", ""))