package test

actual class FooCl
actual object FooObj

fun use() {
    Fo<caret>
}

// EXIST: { lookupString: FooCl, module: testModule_JS }
// EXIST: { lookupString: FooObj, module: testModule_JS }
// ABSENT: { lookupString: FooCl, module: testModule_Common }
// ABSENT: { lookupString: FooObj, module: testModule_Common }