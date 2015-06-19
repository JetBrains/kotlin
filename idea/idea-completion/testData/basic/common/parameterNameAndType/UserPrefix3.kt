package pack

class FooFaa

class Fuu

fun f(myFooF<caret>)

// EXIST: { lookupString: "FooFaa", itemText: "myFooFaa: FooFaa", tailText: " (pack)" }
// EXIST: { lookupString: "Fuu", itemText: "myFooFuu: Fuu", tailText: " (pack)" }
// ABSENT: { itemText: "myFooFooFaa: FooFaa" }
