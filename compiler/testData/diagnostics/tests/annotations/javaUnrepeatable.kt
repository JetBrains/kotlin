// FILE: UnrepeatableAnnotation.java

public @interface UnrepeatableAnnotation {

}

// FILE: UnrepeatableUse.kt

@UnrepeatableAnnotation <!REPEATED_ANNOTATION!>@UnrepeatableAnnotation<!> class My