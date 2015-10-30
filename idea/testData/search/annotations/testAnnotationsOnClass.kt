@java.lang.Deprecated class TestClass1 {}

@java.lang.Deprecated class TestClass2 {}

@java.lang.Deprecated class TestClass3 {}

@java.lang.Deprecated class TestClass4 {}

class TestClass5 {
    @java.lang.Deprecated class innerTestClass5() {}
}

// ANNOTATION: java.lang.Deprecated
// SEARCH: class:TestClass1, class:TestClass2, class:TestClass3
// SEARCH: class:TestClass4
// SEARCH: class:innerTestClass5