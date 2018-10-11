package test

expect fun foo()

fun use() {
    foo<caret>
}

// ABSENT: { lookupString: foo, module: testModule_JVM }
// ABSENT: { lookupString: foo, module: testModule_JS }
// EXIST: { lookupString: foo, module: testModule_Common }