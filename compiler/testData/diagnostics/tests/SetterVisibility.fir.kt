class My {
    var x: Int = 0
        // Ok
        private set
    
    private var y: Int = 1
        // Error: better
        public set

    protected var z: Int = 2
        // Ok
        private set

    protected var w: Int = 3
        // Error: incompatible
        internal set

    internal var v: Int = 4
        // Error: incompatible
        protected set

    internal var t: Int = 5
        // Error: better
        public set
}