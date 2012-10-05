import org.junit.Test

class MyTestClass {
    [org.junit.Test] fun test1() {}

    org.junit.Test fun test2() {}

    [Test] fun test3() {}

    Test fun test4() {}

    [Deprecated org.junit.Test] fun test5() {}

    [Deprecated Test] fun test6() {}

    fun test7() {}
}

// ANNOTATION: org.junit.Test
// SEARCH: PsiMethod:test1, PsiMethod:test2, PsiMethod:test3, PsiMethod:test4, PsiMethod:test5, PsiMethod:test6