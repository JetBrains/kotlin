class Test {

    default object {

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


fun box(): String {
    val t = Test;

    if (t.prop1 != 10) return "fail1";

    if (t.prop2 != 11) return "fail2";

    if (t.prop3 != 12) return "fail3";

    if (t.prop4 != 13) return "fail4";

    t.incProp4()
    if (t.prop4 != 14) return "fail4.inc";

    if (t.prop5 != 14) return "fail5";

    t.prop5 = 1414
    if (t.prop5 != 1414) return "fail6";

    if (t.prop7 != 20) return "fail7";

    t.prop7 = 1000000
    if (t.prop7 != 21) return "fail8";

    return "OK"
}