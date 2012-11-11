package first

fun testFun() {
    NamedObject<caret>
}

// EXIST: NamedObjectTopLevel1, NamedObjectTopLevel2
// ABSENT: NamedObjectInClassObject
// ABSENT: NamedObjectInFun
