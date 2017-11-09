package buz

fun foo() {
    Bar.<caret>
}

// EXIST: {"lookupString":"inBarPackage","tailText":"() for Bar in bar"}
// EXIST: {"lookupString":"Bar","tailText":" (bar.Bar) for Bar in bar"}
// EXIST: {"lookupString":"inFooBarPackage","tailText":"() for Companion in foobar.Bar"}
// EXIST: {"lookupString":"FooBar","tailText":" (foobar.Bar) for Bar in foobar"}
// EXIST: {"lookupString":"inFooPackage","tailText":"() for Companion in foo.Bar"}
// EXIST: {"lookupString":"Foo","tailText":" (foo.Bar) for Bar in foo"}
// EXIST: equals
// EXIST: hashCode
// EXIST: toString
// EXIST: {"lookupString":"inJavapackage","tailText":"() for Bar in javapackage"}
// EXIST: {"lookupString":"JavapackageC","tailText":" (javapackage.Bar)"}
// EXIST: {"lookupString":"Companion","tailText":" (foo.Bar) for Bar in foo"}
// EXIST: {"lookupString":"Companion","tailText":" (foobar.Bar) for Bar in foobar"}



// NOTHING_ELSE
