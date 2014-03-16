package test

import java.util.*

public open class WrongFieldMutability : Object() {
    public var fooNotFinal : String? = ""
    public val fooFinal : String? = "Test"
}
