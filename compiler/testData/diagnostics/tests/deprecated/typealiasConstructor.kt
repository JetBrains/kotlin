@Deprecated("Deprecated class")
open class DeprecatedClass

open class WithDeprecatedCtor(val x: Int) {
    @Deprecated("Deprecated constructor")
    constructor() : this(0)
}

typealias DeprecatedClassAlias = <!DEPRECATION!>DeprecatedClass<!>
typealias WithDeprecatedCtorAlias = WithDeprecatedCtor

class Test1 : <!DEPRECATION!>DeprecatedClassAlias<!>()

class Test2 : <!DEPRECATION!>WithDeprecatedCtorAlias<!>()