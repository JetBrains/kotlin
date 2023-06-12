// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-296
 * MAIN LINK: type-system, type-kinds, built-in-types, kotlin.any -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: type-system, introduction-1 -> paragraph 7 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: The use of Boolean literals as the identifier (with backtick) in the class.
 * HELPERS: checkType, functions
 */

// TESTCASE NUMBER: 1
fun case_1() {
    checkSubtype<Any>("")
    val x: Any = ""
    funWithAnyArg("")
    checkSubtype<Any>("""
        ………
    """)
    val y: Any = """0"""
    funWithAnyArg("""
        =====
    """.trimIndent())
}

// TESTCASE NUMBER: 2
fun case_2() {
    checkSubtype<Any>(0)
    fun z(): Any = 0
    funWithAnyArg(0)
}

// TESTCASE NUMBER: 3
fun case_3() {
    checkSubtype<Any>(-0f)
    fun z(): Any = -.10000e1
    funWithAnyArg(10.1)
}

// TESTCASE NUMBER: 4
fun case_4() {
    checkSubtype<Any>(throw Exception())
    var z1: Any = return@case_4
    while (true) {
        funWithAnyArg(break)
    }
    checkSubtype<Any>(null!!)
    fun z2(): Any = null!!
    funWithAnyArg(null!!)
}

// TESTCASE NUMBER: 5
fun case_5() {
    checkSubtype<Any>('1')
    var z: Any = 'a'
    funWithAnyArg('…')
}

// TESTCASE NUMBER: 6
fun case_6() {
    checkSubtype<Any>({})
    val z: Any = { x: Any -> println(x) }
    funWithAnyArg {}
}

// TESTCASE NUMBER: 7
fun case_7() {
    checkSubtype<Any>(object {})
    val z: Any = { x: Any -> object { val y = x } }(10)
    funWithAnyArg(object : Comparable<Any> {
        override fun compareTo(other: Any) = TODO()
    })
}

// TESTCASE NUMBER: 8
fun case_8() {
    checkSubtype<Any>(object {}::class)
    val z: Any = {}::class
    funWithAnyArg(0E0::class)
}

// TESTCASE NUMBER: 9
fun case_9() {
    checkSubtype<Any>(object {}::class)
    val z: Any = {}::class
    funWithAnyArg(0E0::class)
}

// TESTCASE NUMBER: 10
fun case_10() {
    checkSubtype<Any>(0 + 0.0)
    val z: Any = if (true) "" else '"'
    label@ {
        funWithAnyArg(return@label)
    }
}

// TESTCASE NUMBER: 11
fun case_11() {
    checkSubtype<Any>(Any())
    val z: Any = if (true) Any() else Any()
    funWithAnyArg(Any())
}
