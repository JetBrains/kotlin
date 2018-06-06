package test

expect class Foo

fun use() {
    Foo<caret>
}

// ABSENT: { lookupString: Foo, module: testModule_JVM }
// ABSENT: { lookupString: Foo, module: testModule_JS }
// EXIST: { lookupString: Foo, module: testModule_Common }