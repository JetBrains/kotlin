import org.junit.Test as unittest
import org.junit.Test

class MyTestClass {
    @unittest fun test1() {}

    @Deprecated @unittest fun test2() {}

    @Test fun test3() {}
}

// ANNOTATION: org.junit.Test
// SEARCH: KotlinLightMethodForDeclaration:test1
// SEARCH: KotlinLightMethodForDeclaration:test2
// SEARCH: KotlinLightMethodForDeclaration:test3