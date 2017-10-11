package codegen.delegatedProperty.observable

import kotlin.test.*

import kotlin.properties.Delegates

class User {
    var name: String by Delegates.observable("<no name>") {
        prop, old, new ->
        println("$old -> $new")
    }
}

@Test fun runTest() {
    val user = User()
    user.name = "first"
    user.name = "second"
}