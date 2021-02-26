// FIR_COMPARISON
package pack

fun testFun() {
    gl<caret>
}

// EXIST: globalProp1, globalProp2
// ABSENT: globalProp3
// ABSENT: globalExtensionProp
// ABSENT: globalExtensionFun
