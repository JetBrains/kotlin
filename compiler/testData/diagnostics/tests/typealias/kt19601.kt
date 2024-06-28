interface Order<T>

typealias Ord<T> = Order<T>

class Test1<T1 : Ord<T1>>

interface Num<T : Number>

typealias N<T> = Num<T>

class Test2<T : <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>N<String><!>>
