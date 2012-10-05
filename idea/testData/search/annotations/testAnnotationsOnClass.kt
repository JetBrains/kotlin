[Deprecated] class TestClass1 {}

[java.lang.Deprecated] class TestClass2 {}

Deprecated class TestClass3 {}

java.lang.Deprecated class TestClass4 {}

class TestClass5 {
    Deprecated class innerTestClass5() {}
}

// ANNOTATION: java.lang.Deprecated
// SEARCH: JetLightClass:TestClass1, JetLightClass:TestClass2, JetLightClass:TestClass3
// SEARCH: JetLightClass:TestClass4
// SEARCH: JetLightClass:TestClass5.innerTestClass5