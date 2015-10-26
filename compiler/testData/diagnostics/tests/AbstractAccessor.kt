abstract class My {
    abstract var x: Int
        <!REDUNDANT_MODIFIER_IN_GETTER!>public<!> get
        <!ACCESSOR_VISIBILITY_FOR_ABSTRACT_PROPERTY!>private<!> set

    abstract val y: Int
        <!GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY!>protected<!> get

    abstract protected var z: Int
        <!GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY!>internal<!> get
        <!ACCESSOR_VISIBILITY_FOR_ABSTRACT_PROPERTY!>private<!> set

    abstract internal val w: Int
        <!GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY!>protected<!> get

    abstract var u: Int
        protected set
}