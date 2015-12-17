class My {

    lateinit var x: String
        private set

    lateinit var y: String
        internal set

    lateinit protected var z: String
        private set

    lateinit private var w: String
        // Ok, private var / private set
        private set

    lateinit protected var v: String
        <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>public<!> set

    lateinit public var u: String
        // Ok, public var / public set
        public set
}