typealias TA<X, Y> = (x: X) -> Y
abstract class Base<X, Y> : TA<X, Y>
<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class Impl<!> : Base<Any, Any>()
