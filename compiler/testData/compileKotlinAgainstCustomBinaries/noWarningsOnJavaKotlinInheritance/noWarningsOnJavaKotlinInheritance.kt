// Simple inheritance. Checks that there's no PARAMETER_NAME_CHANGED_ON_OVERRIDE warning

class SimpleSubclass : JavaInterface {
    override fun foo(kotlinName: Int) {}
}


// Class extends both Java and Kotlin traits. Checks that there's no DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES warning

interface KotlinTrait {
    public fun foo(someOtherName: Int) {}
}

class BothTraitsSubclass : JavaInterface, KotlinTrait {
    override fun foo(someOtherName: Int) {
        super.foo(someOtherName)
    }
}
