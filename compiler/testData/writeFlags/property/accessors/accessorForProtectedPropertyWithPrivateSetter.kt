// FILE: A.kt
package a

abstract class A {
    protected var property: String = ""
        private set
}

// FILE: B.kt
package b

import a.A

class B : A() {
    init {
        invoke { property }
    }

    fun invoke(func: () -> String): String = func()
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: b/B, access$setProperty$p
// ABSENT: true