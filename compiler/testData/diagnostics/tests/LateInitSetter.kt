class My {

    lateinit var x: String
        <!PRIVATE_SETTER_ON_NON_PRIVATE_LATE_INIT_VAR!>private<!> set

    lateinit var y: String
        // Ok, non-private setter
        internal set

    lateinit protected var z: String
        <!PRIVATE_SETTER_ON_NON_PRIVATE_LATE_INIT_VAR!>private<!> set

    lateinit private var w: String
        // Ok, private var
        private set
}