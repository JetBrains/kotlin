abstract class ReadNonexistent() {
    abstract val aa: Int
    
    {
        val x = <!NO_BACKING_FIELD_ABSTRACT_PROPERTY!>$aa<!>
    }
}
