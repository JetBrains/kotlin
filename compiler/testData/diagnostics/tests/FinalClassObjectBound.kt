class Other
trait Trait
trait WithBounds<T: Trait>
class Test1<T> where <!UNSUPPORTED!>class object T: WithBounds<<!UPPER_BOUND_VIOLATED!>Other<!>><!>