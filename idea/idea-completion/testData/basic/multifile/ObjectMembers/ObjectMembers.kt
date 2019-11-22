package first

fun testFun() {
    f<caret>
}

// INVOCATION_COUNT: 2
// EXIST: { allLookupStrings: "funFromObject", itemText: "KotlinObject.funFromObject", tailText: "() (test)", typeText: "Unit", attributes: "" }
// EXIST: { allLookupStrings: "funFromCompanionObject", itemText: "KotlinClass.funFromCompanionObject", tailText: "() (test)", typeText: "Unit", attributes: "" }
// EXIST: { allLookupStrings: "fromNested", itemText: "Nested.fromNested", tailText: "() (test.AnotherKotlinClass)", typeText: "Unit", attributes: "" }
// EXIST: { allLookupStrings: "fromInterface", itemText: "DefaultI.fromInterface", tailText: "() (test)", typeText: "Unit", attributes: "" }
// ABSENT: funPrivate
// ABSENT: funFromPrivateCompanionObject
// ABSENT: fromAnonymous
