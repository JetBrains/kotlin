package jvmTest

import common.greetEachOther
import some.example.Person

private class MyPerson(
    name: String
) : Person(name) {
    override fun greet() = "Hi"
}

fun test() {
    greetEachOther(
        listOf(
            Person("Alice"),
            MyPerson("Bob"),
        )
    )
}