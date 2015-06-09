@Deprecated class TestClass1 {}

@java.lang.Deprecated class TestClass2 {}

Deprecated class TestClass3 {}

java.lang.Deprecated class TestClass4 {}

class TestClass5 {
    Deprecated class innerTestClass5() {}
}

// ANNOTATION: java.lang.Deprecated
// SEARCH: KotlinLightClass:TestClass1, KotlinLightClass:TestClass2, KotlinLightClass:TestClass3
// SEARCH: KotlinLightClass:TestClass4
// SEARCH: KotlinLightClass:TestClass5.innerTestClass5