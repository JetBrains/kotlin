import java.lang.annotation.ElementType

fun foo(){
    var e : ElementType? = <caret>
}

// EXIST: { lookupString:"ElementType.TYPE", itemText:"ElementType.TYPE", tailText:" (java.lang.annotation)", typeText:"java.lang.annotation.ElementType" }
// EXIST: { lookupString:"ElementType.FIELD", itemText:"ElementType.FIELD", tailText:" (java.lang.annotation)", typeText:"java.lang.annotation.ElementType" }
