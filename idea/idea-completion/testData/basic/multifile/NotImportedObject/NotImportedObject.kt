// FIR_COMPARISON
package first

fun testFun() {
    NamedObject<caret>
}

// EXIST: NamedObjectTopLevel1, NamedObjectTopLevel2, NamedObjectInClassObject
// ABSENT: NamedObjectInFun
