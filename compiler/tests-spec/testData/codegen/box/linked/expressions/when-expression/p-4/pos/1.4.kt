// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, when-expression -> paragraph 4 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION:  it is possible to  replace the else condition with an always-true condition (sealed class)
 */

sealed class SClass {
    class A : SClass()
    class B : SClass()
    class C : SClass()
}

fun box(): String {
    val x: SClass = SClass.B()

    val when1 = when (x){
        is  SClass.A ->{ "NOK"}
        is  SClass.B ->{ "OK" }
        is  SClass.B ->{ "NOK" }
        is  SClass.C ->{ "NOK" }
    }
    return when1
}