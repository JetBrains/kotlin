// FIR_IDENTICAL

// MODULE: lib
// FILE: kotlin1.kt
@file:OptIn(ExperimentalObjCName::class)

package example
import kotlin.experimental.ExperimentalObjCName

interface BaseInterface {
    @kotlin.native.ObjCName("getValue")
    fun getValue(): Int
}


// MODULE: main(lib)
// FILE: kotlin2.kt

@file:OptIn(ExperimentalObjCName::class)

package example
import kotlin.experimental.ExperimentalObjCName

open class BaseClass {
    @kotlin.native.ObjCName("getValue")
    fun getValue(): Int {
        return 0
    }
}

// FILE: kotlin3.kt

@file:OptIn(ExperimentalObjCName::class)

package example
import kotlin.experimental.ExperimentalObjCName

class Derived: BaseClass(), BaseInterface {}