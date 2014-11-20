fun foo(){
    var l : java.util.Locale = <caret>
}

// EXIST: { lookupString:"ENGLISH", itemText:"Locale.ENGLISH", tailText:" (java.util)", typeText:"Locale!", attributes:"" }
// EXIST: { lookupString:"FRENCH", itemText:"Locale.FRENCH", tailText:" (java.util)", typeText:"Locale!", attributes:"" }
