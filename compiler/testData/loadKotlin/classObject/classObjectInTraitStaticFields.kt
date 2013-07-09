package test

trait Test {

    class object {

        public val prop1 : Int = 10

        public var  prop2 : Int = 11
            protected set

        public val prop3: Int = 12
            get() {
                return  $prop3
            }

        var prop4 : Int = 13

        fun incProp4() {
            $prop4++
        }

        public var prop5 : Int = 14

        public var prop7 : Int = 20
            set(i: Int) {
                $prop7++
            }
    }
}