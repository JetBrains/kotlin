abstract class My {
    abstract var x: Int
        public get
        private set

    abstract val y: Int
        protected get

    abstract protected var z: Int
        internal get
        private set

    abstract internal val w: Int
        protected get

    abstract var u: Int
        protected set

    open var t: Int = 0
        private set
}