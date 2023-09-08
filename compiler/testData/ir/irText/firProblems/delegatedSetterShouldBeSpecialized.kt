// FIR_IDENTICAL
// WITH_REFLECT
// SKIP_SIGNATURE_DUMP

var topLevelInt: Int = 0

class MyClass {
    var delegatedToTopLevel: Int by ::topLevelInt
}
