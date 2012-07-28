class CustomValNoBackingField() {
    val a: Int
        get() = 1
    
    val b = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$a<!>
}
