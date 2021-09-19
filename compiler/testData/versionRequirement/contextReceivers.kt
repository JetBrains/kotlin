package test

interface A
interface B

context(A, B)
class ClassWithCR {
    context(A, B)
    val memberPropWithCR: Any?
        get() = null

    context(A, B)
    fun memberFunWithCR() {}
}

context(A, B)
fun topLevelFunWithCR() {}

context(A, B)
val topLevelPropWithCR: Any?
    get() = null
