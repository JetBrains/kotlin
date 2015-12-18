class My {
    var x: Int = 0
        // Ok
        private set
    
    private var y: Int = 1
        // Error: better
        <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>public<!> set

    protected var z: Int = 2
        // Ok
        private set

    protected var w: Int = 3
        // Error: incompatible
        <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>internal<!> set

    internal var v: Int = 4
        // Error: incompatible
        <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>protected<!> set

    internal var t: Int = 5
        // Error: better
        <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>public<!> set
}