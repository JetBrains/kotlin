class My {

    lateinit var x: String
        <!SETTER_VISIBILITY_DIFFERS_FROM_LATEINIT_VISIBILITY!>private<!> set

    lateinit var y: String
        <!SETTER_VISIBILITY_DIFFERS_FROM_LATEINIT_VISIBILITY!>internal<!> set

    lateinit protected var z: String
        <!SETTER_VISIBILITY_DIFFERS_FROM_LATEINIT_VISIBILITY!>private<!> set

    lateinit private var w: String
        // Ok, private var / private set
        private set

    lateinit protected var v: String
        <!SETTER_VISIBILITY_DIFFERS_FROM_LATEINIT_VISIBILITY!>public<!> set

    lateinit public var u: String
        // Ok, public var / public set
        public set
}