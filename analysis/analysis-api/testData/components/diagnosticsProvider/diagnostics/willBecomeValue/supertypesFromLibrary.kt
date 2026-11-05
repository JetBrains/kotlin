// WITH_STDLIB
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
@file:OptIn(ExperimentalValueClassesApi::class)

package lib

@WillBecomeValue
abstract class OverridingBase {
    override fun equals(other: Any?): Boolean = other is OverridingBase
    override fun hashCode(): Int = 0
    override fun toString(): String = "OverridingBase"
}

@WillBecomeValue
abstract class NotOverridingBase

abstract class IdentityBase

// MODULE: main(lib)
// FILE: main.kt
@file:OptIn(ExperimentalValueClassesApi::class)

import lib.IdentityBase
import lib.NotOverridingBase
import lib.OverridingBase

@WillBecomeValue
class OfOverridingBase(val x: Int) : OverridingBase()

@WillBecomeValue
class OfNotOverridingBase(val x: Int) : NotOverridingBase()

@WillBecomeValue
class OfIdentityBase(val x: Int) : IdentityBase() {
    override fun equals(other: Any?): Boolean = other is OfIdentityBase && x == other.x
    override fun hashCode(): Int = x
    override fun toString(): String = "OfIdentityBase($x)"
}

class NotAnnotatedOfNotOverridingBase(val x: Int) : NotOverridingBase()
