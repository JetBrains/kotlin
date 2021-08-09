abstract class Some1 {
    abstract val foo: List<Int>
        <!EXPLICIT_BACKING_FIELD_IN_ABSTRACT_PROPERTY!>internal field = mutableListOf<Int>()<!>
}
