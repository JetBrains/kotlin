open class C<T : C<T>>
class TestOK : C<TestOK>()
class TestFail : <!UPPER_BOUND_VIOLATED!>C<C<TestFail>><!>()
