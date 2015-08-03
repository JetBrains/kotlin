package test

target(AnnotationTarget.VALUE_PARAMETER)
annotation class Ann

class A {

    @receiver:Ann
    fun String.myLength(@Ann q:String): Int {
        return length()
    }

    @receiver:Ann
    val String.myLength2: Int
        get() = length()

    @receiver:Ann
    var String.myLength3: Int
        get() = length()
        set(v) {}

}