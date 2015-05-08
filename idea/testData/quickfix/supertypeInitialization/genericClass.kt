// "Add constructor parameters from Base(p1: T, p2: String, p3: Base<T, String>?)" "true"
trait I

open class Base<T1, T2>(p1: T1, p2: T2, p3: Base<T1, T2>?)

class C<T> : I, Base<T, String><caret>