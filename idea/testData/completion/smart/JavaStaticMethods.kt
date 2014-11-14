fun foo(){
    val l : java.util.Calendar = <caret>
}

// EXIST: { lookupString:"getInstance", itemText:"Calendar.getInstance", tailText:"() (java.util)", typeText:"Calendar!", attributes:"" }
// EXIST: { lookupString:"getInstance", itemText:"Calendar.getInstance", tailText:"(TimeZone!) (java.util)", typeText:"Calendar!", attributes:"" }
// EXIST: { lookupString:"getInstance", itemText:"Calendar.getInstance", tailText:"(TimeZone!, Locale!) (java.util)", typeText:"Calendar!", attributes:"" }
