// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// see https://youtrack.jetbrains.com/issue/KT-21515

interface SomeIrrelevantInterface

// note that C.Base() supertype will be resolved in normal scope
abstract class DerivedAbstract : C.Base()

class Data

public class C {

    val data: Data = Data()

    // Note that any supertype of Base will be resolved in error-scope, even if it absolutely irrelevant
    // to the types in cycle.
    open class Base() : SomeIrrelevantInterface

    companion object : DerivedAbstract()
}
