// LANGUAGE: -AllowSealedInheritorsInDifferentFilesOfSamePackage
// DIAGNOSTICS: -UNUSED_PARAMETER

sealed class Case1(val x: Int) {
    <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>protected<!> constructor(s: String) : this(s.length)

    class Inheritor1 : Case1(10)
    class Inheritor2 : Case1("Hello")
}

sealed class Case2 <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>protected<!> constructor(val x: Int) {
    <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>protected<!> constructor(s: String) : this(s.length)

    class Inheritor1 : Case2(10)
    class Inheritor2 : Case2("Hello")
}

sealed class Case3 private constructor(val x: Int) {
    <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>protected<!> constructor(s: String) : this(s.length)

    class Inheritor1 : Case3(10) // should OK
    class Inheritor2 : Case3("Hello")
}

class Case3Inheritor3 : Case3(20) // should be an error in 1.6 (?)

sealed class Case4 {
    <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>protected<!> constructor(x: Int)
    <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>protected<!> constructor(s: String) : this(s.length)

    class Inheritor1 : Case4(10)
    class Inheritor2 : Case4("Hello")
}

sealed class Case5() {
    private constructor(x: Int) : this()
    <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>protected<!> constructor(x: Byte) : this()
    <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>internal<!> constructor(x: Short) : this()
    <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>public<!> constructor(x: Long) : this()
    constructor(x: Double) : this()
}
