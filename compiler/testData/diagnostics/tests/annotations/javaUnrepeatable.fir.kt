// FILE: UnrepeatableAnnotation.java

public @interface UnrepeatableAnnotation {

}

// FILE: UnrepeatableUse.kt

@UnrepeatableAnnotation @UnrepeatableAnnotation class My