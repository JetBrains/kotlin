package common

import some.example.Person

fun greetEachOther(people: Collection<Person>) {
    for (person in people) {
        person.greet()
    }
}