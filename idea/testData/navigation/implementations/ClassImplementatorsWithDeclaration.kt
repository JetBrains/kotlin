package testing

open class Test

open class TestOther : <caret>Test()

class TestOtherMore : TestOther()

// REF: Test
// REF: TestOther
// REF: TestOtherMore
