package testing

open class <caret>Test

open class TestOther : Test()

class TestOtherMore : TestOther()

// REF: TestOtherMore
// REF: TestOther
