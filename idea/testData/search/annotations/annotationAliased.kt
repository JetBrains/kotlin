import java.lang.Deprecated as D

@D class TestClass1 {}


@D fun f()

class TestClass5 {
    @D fun g() {}

    @D class innerTestClass() {}
}

// ANNOTATION: java.lang.Deprecated
// SEARCH: class:TestClass1, class:innerTestClass, method:f, method:g
