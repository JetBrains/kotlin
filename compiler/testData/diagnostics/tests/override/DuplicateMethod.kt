// RUN_PIPELINE_TILL: FRONTEND
interface Some {
    fun test()
}

class SomeImpl : Some  {
    <!CONFLICTING_OVERLOADS!>override fun test()<!> {}
    <!CONFLICTING_OVERLOADS!>override fun test()<!> {}
}