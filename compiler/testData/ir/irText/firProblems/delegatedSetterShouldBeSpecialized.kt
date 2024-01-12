// FIR_IDENTICAL
// WITH_REFLECT

var topLevelInt: Int = 0

class MyClass {
    var delegatedToTopLevel: Int by ::topLevelInt
}
