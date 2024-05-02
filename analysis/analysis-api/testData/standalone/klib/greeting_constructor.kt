// MODULE: lib-common

// FILE: com/hello/Hello.kt
package com.hello

interface Hello {
    val hello : String
    fun greet(name : String) : String
}

// FILE: com/hello/Person.kt
package com.hello

data class Person(val name : String, val age : Int) : Hello {
    override val hello get() = "hi"
    override fun greet(name : String) = "${this.name} $hello $name!"
}

// MODULE: commonMain(lib-common)
// MODULE_KIND: Source
// FILE: main.kt

import com.hello.Person

fun test() : Any = Per<caret>son("Foo", 10).greet("Bar")
