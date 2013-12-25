fun foo(){
    val l : java.util.Calendar = <caret>
}

// EXIST: { lookupString:"Calendar.getInstance", itemText:"Calendar.getInstance", tailText:"() (java.util)", typeText:"Calendar" }
// EXIST: { lookupString:"Calendar.getInstance", itemText:"Calendar.getInstance", tailText:"(TimeZone) (java.util)", typeText:"Calendar" }
// EXIST: { lookupString:"Calendar.getInstance", itemText:"Calendar.getInstance", tailText:"(TimeZone, Locale) (java.util)", typeText:"Calendar" }
