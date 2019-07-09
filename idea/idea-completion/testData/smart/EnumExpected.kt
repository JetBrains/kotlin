package sample

enum class Foo {
    X, Y
}

fun foo(){
    val f : Foo = <caret>
}

// WITH_ORDER
// EXIST: { lookupString:"X", itemText:"Foo.X", tailText:" (sample)", typeText:"Foo" }
// EXIST: { lookupString:"Y", itemText:"Foo.Y", tailText:" (sample)", typeText:"Foo" }
// EXIST: {"lookupString":"enumValueOf","tailText":"(name: String) (kotlin)","typeText":"Foo","attributes":"","allLookupStrings":"enumValueOf","itemText":"enumValueOf"}
// EXIST: {"lookupString":"maxOf","tailText":"(a: Foo, b: Foo) (kotlin.comparisons)","typeText":"Foo","attributes":"","allLookupStrings":"maxOf","itemText":"maxOf"}
// EXIST: {"lookupString":"maxOf","tailText":"(a: Foo, b: Foo, c: Foo) (kotlin.comparisons)","typeText":"Foo","attributes":"","allLookupStrings":"maxOf","itemText":"maxOf"}
// EXIST: {"lookupString":"minOf","tailText":"(a: Foo, b: Foo) (kotlin.comparisons)","typeText":"Foo","attributes":"","allLookupStrings":"minOf","itemText":"minOf"}
// EXIST: {"lookupString":"minOf","tailText":"(a: Foo, b: Foo, c: Foo) (kotlin.comparisons)","typeText":"Foo","attributes":"","allLookupStrings":"minOf","itemText":"minOf"}
// EXIST: {"lookupString":"valueOf","tailText":"(value: String) (sample)","typeText":"Foo","module":"light_idea_test_case","attributes":"","allLookupStrings":"Foo, valueOf","itemText":"Foo.valueOf"}

// NOTHING_ELSE
