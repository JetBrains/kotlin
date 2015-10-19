@java.lang.Deprecated fun test1() {}

@java.lang.Deprecated fun test2() {}

fun test3() {
  @java.lang.Deprecated fun test3inner() {}
}

class Test4() {
    @java.lang.Deprecated fun test4() {}
}

class Test5() {
    fun test5() {
        @java.lang.Deprecated fun test5inner() {}
    }
}

class Test6() {
    companion object {
        @java.lang.Deprecated fun test6() {}
    }
}

object Test7 {
    @java.lang.Deprecated fun test7() {}
}

// ANNOTATION: java.lang.Deprecated
// SEARCH: KotlinLightMethodForDeclaration:test1
// SEARCH: KotlinLightMethodForDeclaration:test2
// SEARCH: KotlinLightMethodForDeclaration:test4
// SEARCH: KotlinLightMethodForDeclaration:test6
// SEARCH: KotlinLightMethodForDeclaration:test7