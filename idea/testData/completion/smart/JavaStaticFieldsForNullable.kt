fun foo(){
    var l : java.util.Locale? = <caret>
}

// EXIST: { lookupString:"Locale.ENGLISH", itemText:"Locale.ENGLISH", tailText:" (java.util)", typeText:"Locale!" }
// EXIST: { lookupString:"Locale.FRENCH", itemText:"Locale.FRENCH", tailText:" (java.util)", typeText:"Locale!" }
