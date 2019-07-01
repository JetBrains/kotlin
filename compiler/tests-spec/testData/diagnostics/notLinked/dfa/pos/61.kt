// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 61
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
class Case1 {
    init {
        var y: String? = "xyz"
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?"), SMARTCAST_IMPOSSIBLE!>y<!>.length
        }
    }
    constructor()
}

// TESTCASE NUMBER: 2
class Case2 {
    init {
        var y: String? = "xyz"
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>y<!>.length
        }
    }
}

// TESTCASE NUMBER: 3
class Case3 {
    init {
        var y: String? = "xyz"
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>y<!>.length
        }
    }
    init { }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
class Case4 {
    init {
        var y: String? = "xyz"
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?"), SMARTCAST_IMPOSSIBLE!>y<!>.length
        }
    }
    constructor(y: Int?) {
        println(y)
    }
}

// TESTCASE NUMBER: 5
class Case5() {
    init {
        var y: String? = "xyz"
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>y<!>.length
        }
    }
    constructor(y: Int?) : this() {
        println(y)
    }
}

// TESTCASE NUMBER: 6
class Case6() {
    init {
        var y: String? = "xyz"
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>y<!>.length
        }
    }
}

// TESTCASE NUMBER: 7
class Case7() {
    init {
        var y: String? = "xyz"
        if (y != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>y<!>.length
        }
    }
    constructor(y: Int?) : this() {
        println(y)
    }
    constructor(y: Int?, z: Int?) : this() {
        println(y)
    }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
class Case8 {
    init {
        var y: String? = "xyz"
        y!!
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?"), SMARTCAST_IMPOSSIBLE!>y<!>.length
    }
    constructor()
}

/*
 * TESTCASE NUMBER: 9
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
class Case9 {
    init {
        var y: String? = "xyz"
        if (y == null) throw Exception()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?"), SMARTCAST_IMPOSSIBLE!>y<!>.length
    }
    constructor()
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
class Case10 {
    init {
        var y: String? = "xyz"
        y ?: throw Exception()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?"), SMARTCAST_IMPOSSIBLE!>y<!>.length
    }
    constructor()
}

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
val case_12: Nothing? get() = null
class Case11 {
    init {
        var y: String? = "xyz"
        if (y == <!DEBUG_INFO_CONSTANT!>case_12<!>)
            throw Exception()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?"), SMARTCAST_IMPOSSIBLE!>y<!>.length
    }
    constructor()
}
