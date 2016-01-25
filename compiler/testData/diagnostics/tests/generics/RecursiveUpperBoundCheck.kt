open class C<T : C<T>>
class TestOK : C<TestOK>()
class TestFail : C<<!UPPER_BOUND_VIOLATED!>C<<!UPPER_BOUND_VIOLATED!>TestFail<!>><!>>()
