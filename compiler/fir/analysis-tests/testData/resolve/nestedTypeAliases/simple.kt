// RUN_PIPELINE_TILL: BACKEND
class A

class C {
    typealias TA = A

    fun test(): TA = TA()
}
