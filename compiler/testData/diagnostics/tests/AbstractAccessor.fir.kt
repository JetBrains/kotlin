abstract class My {
    abstract var x: Int
        public get
        <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set

    abstract val y: Int
        <!GETTER_VISIBILITY_SMALLER_THAN_PROPERTY_VISIBILITY, REDUNDANT_GETTER_VISIBILITY_CHANGE!>protected<!> get

    abstract protected var z: Int
        <!REDUNDANT_GETTER_VISIBILITY_CHANGE!>internal<!> get
        <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set

    abstract internal val w: Int
        <!REDUNDANT_GETTER_VISIBILITY_CHANGE!>protected<!> get

    abstract var u: Int
        protected set

    open var t: Int = 0
        <!PRIVATE_SETTER_FOR_OPEN_PROPERTY!>private<!> set
}
