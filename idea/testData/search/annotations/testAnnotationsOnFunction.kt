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
    default object {
        Deprecated fun test6() {}
    }
}

object Test7 {
    Deprecated fun test7() {}
}

// ANNOTATION: java.lang.Deprecated
// SEARCH: PsiMethod:test1, PsiMethod:test2, PsiMethod:test4, PsiMethod:test6, PsiMethod:test7