// PLATFORM_DEPENDANT_METADATA
//ALLOW_AST_ACCESS
package test

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Ann

class A {

    fun @receiver:Ann String.myLength(@Ann q:String): Int {
        return length
    }

    val @receiver:Ann String.myLength2: Int
        get() = length

    var @receiver:[Ann] String.myLength3: Int
        get() = length
        set(v) {}

}
