val foo = object Name where T : G {}
val foo = object : Bar where T : G {}

val foo = object() where T : G {}
val foo = object() : Bar where T : G {}

val foo = object() : Bar where T : G {}