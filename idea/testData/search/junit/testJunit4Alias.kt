import org.junit.Test as test
import org.junit.Test

class MyTestClass {
    test fun test1() {}

    [Deprecated test] fun test2() {}

    Test fun test3() {}
}

// ANNOTATION: org.junit.Test
// SEARCH: PsiMethod:test1, PsiMethod:test2, PsiMethod:test3