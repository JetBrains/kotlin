//package a {
    val afoo = <error descr="[TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM] Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly">abar()</error>

    fun abar() = <error descr="[DEBUG] Resolved to error element">afoo</error>
//}

//package b {
    fun bfoo() = bbar()

    fun bbar() = <error descr="[TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM] Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly">bfoo()</error>
//}

//package c {
    fun cbazz() = cbar()

    fun cfoo() = <error descr="[TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM] Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly">cbazz()</error>

    fun cbar() = cfoo()
//}

//package ok {
//
//    package a {
        val okafoo = okabar()

        fun okabar() : Int = okafoo
//    }
//
//    package b {
        fun okbfoo() : Int = okbbar()

        fun okbbar() = okbfoo()
//    }
//
//    package c {
        fun okcbazz() = okcbar()

        fun okcfoo() : Int = okcbazz()

        fun okcbar() = okcfoo()
//    }
//}
