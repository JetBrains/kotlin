package first

fun testFun() {
    funFromO<caret>
}

// INVOCATION_COUNT: 2
// EXIST: { allLookupStrings: "funFromObject", itemText: "KotlinObject.funFromObject", tailText: "() (test)", typeText: "Unit", attributes: "" }
// EXIST: { allLookupStrings: "funFromCompanionObject", itemText: "KotlinClass.funFromCompanionObject", tailText: "() (test)", typeText: "Unit", attributes: "" }
// ABSENT: privateFun
// ABSENT: funFromPrivateCompanionObject
