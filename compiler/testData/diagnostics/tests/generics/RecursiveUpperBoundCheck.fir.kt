open class C<T : C<T>>
class TestOK : C<TestOK>()
class TestFail : C<C<TestFail>>()
