class Person(val `super`: String)

val reader = { p: Person -> p.`super`<caret> }