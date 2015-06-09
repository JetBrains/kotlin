import org.junit.Test

class MyTestClass {
    @org.junit.Test fun test1() {}

    org.junit.Test fun test2() {}

    @Test fun test3() {}

    Test fun test4() {}

    @Deprecated @org.junit.Test fun test5() {}

    @Deprecated @Test fun test6() {}

    fun test7() {}
}

// ANNOTATION: org.junit.Test
// SEARCH: KotlinLightMethodForDeclaration:test1
// SEARCH: KotlinLightMethodForDeclaration:test2
// SEARCH: KotlinLightMethodForDeclaration:test3
// SEARCH: KotlinLightMethodForDeclaration:test4
// SEARCH: KotlinLightMethodForDeclaration:test5
// SEARCH: KotlinLightMethodForDeclaration:test6