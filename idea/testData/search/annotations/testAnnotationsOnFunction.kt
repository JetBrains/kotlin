Deprecated fun test1() {}

java.lang.Deprecated fun test2() {}

fun test3() {
  Deprecated fun test3inner() {}
}

class Test4() {
    Deprecated fun test4() {}
}

class Test5() {
    fun test5() {
        Deprecated fun test5inner() {}
    }
}

class Test6() {
    companion object {
        Deprecated fun test6() {}
    }
}

object Test7 {
    Deprecated fun test7() {}
}

// ANNOTATION: java.lang.Deprecated
// SEARCH: KotlinLightMethodForDeclaration:test1
// SEARCH: KotlinLightMethodForDeclaration:test2
// SEARCH: KotlinLightMethodForDeclaration:test4
// SEARCH: KotlinLightMethodForDeclaration:test6
// SEARCH: KotlinLightMethodForDeclaration:test7