abstract class My {
    abstract var x: Int
        <!REDUNDANT_MODIFIER_IN_GETTER!>public<!> get
        <!ACCESSOR_VISIBILITY_FOR_ABSTRACT_PROPERTY!>private<!> set

    abstract val y: Int
        <!ACCESSOR_VISIBILITY_FOR_ABSTRACT_PROPERTY!>protected<!> get

    abstract protected var z: Int
        <!ACCESSOR_VISIBILITY_FOR_ABSTRACT_PROPERTY!>internal<!> get
        <!ACCESSOR_VISIBILITY_FOR_ABSTRACT_PROPERTY!>public<!> set

    abstract internal val w: Int
        <!ACCESSOR_VISIBILITY_FOR_ABSTRACT_PROPERTY!>protected<!> get
}