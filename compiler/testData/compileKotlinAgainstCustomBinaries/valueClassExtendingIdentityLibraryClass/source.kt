package test

import lib.*

value class FromIdentity(override val x: Int) : IdentityBase()

value class FromValue(override val x: Int) : ValueBase()

value class FromJvm17(override val x: Int) : Jvm17Base()

// `Jdk27Base` is a value class in a class file of JDK 27, which the test patches.
value class FromJdk27(override val x: Int) : Jdk27Base()

class IdentityFromIdentity(override val x: Int) : IdentityBase()
