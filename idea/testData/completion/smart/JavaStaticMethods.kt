fun foo(){
    val l : java.lang.Thread = <caret>
}

// EXIST: { lookupString:"Thread.currentThread", itemText:"Thread.currentThread()", tailText:" (java.lang)", typeText:"Thread" }
