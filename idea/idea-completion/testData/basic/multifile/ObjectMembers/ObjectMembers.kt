package first

fun testFun() {
    f<caret>
}

// INVOCATION_COUNT: 2
// EXIST: { allLookupStrings: "funFromObject", itemText: "KotlinObject.funFromObject", tailText: "() (test)", typeText: "Unit", attributes: "" }
// EXIST: { allLookupStrings: "funFromCompanionObject", itemText: "KotlinClass.funFromCompanionObject", tailText: "() (test)", typeText: "Unit", attributes: "" }
// ABSENT: funPrivate
// ABSENT: funFromPrivateCompanionObject
// ABSENT: fromNested
// ABSENT: fromInterface
// ABSENT: fromAnonymous
