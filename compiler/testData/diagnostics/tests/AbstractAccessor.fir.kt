abstract class My {
    abstract var x: Int
        public get
        <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set

    abstract val y: Int
        protected get

    abstract protected var z: Int
        internal get
        <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set

    abstract internal val w: Int
        protected get

    abstract var u: Int
        protected set

    open var t: Int = 0
        <!PRIVATE_SETTER_FOR_OPEN_PROPERTY!>private<!> set
}
