fun String?.foo(){
    val s : String = <caret>
}

// EXIST: { lookupString:"this", itemText: "!! this", typeText:"String?" }
// EXIST: { lookupString:"this", itemText: "?: this", typeText:"String?" }
