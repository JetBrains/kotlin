//KT-4321 invoke() on enum doesn't work

import DOMElementTestClasses.cls2

// use case 1
enum class DOMElementTestClasses {
    cls1, cls2;

    operator fun invoke() {}
}


// use case 2
interface EnumStyleClass {
    operator fun invoke() {}
}
enum class TestClasses : EnumStyleClass {
    cls
}

// example
fun main() {
    // Kotlin: Expression 'cls1' of type 'DOMElementTestClasses' cannot be invoked as a function
    DOMElementTestClasses.cls1()

    // Kotlin: Expression 'cls2' of type 'DOMElementTestClasses' cannot be invoked as a function
    cls2()

    // Kotlin: Expression 'cls' of type 'TestClasses' cannot be invoked as a function
    TestClasses.cls()

    // All ok
    val cls = DOMElementTestClasses.cls2
    cls()
}