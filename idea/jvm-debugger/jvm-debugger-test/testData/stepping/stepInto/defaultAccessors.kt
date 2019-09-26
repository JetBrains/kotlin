package defaultAccessors

fun main(args: Array<String>) {
    //Breakpoint!
    A().testPublicPropertyInClass()
    testPublicPropertyInLibrary()
}

class A: B() {
    fun testPublicPropertyInClass() {
        prop
        prop = 2
    }
}

open class B {
    public var prop: Int = 1
}

fun testPublicPropertyInLibrary() {
    val myClass = customLib.simpleLibFile.B()
    myClass.prop
    myClass.prop = 2
}

// STEP_INTO: 21
// SKIP_SYNTHETIC_METHODS: true
// SKIP_CONSTRUCTORS: true