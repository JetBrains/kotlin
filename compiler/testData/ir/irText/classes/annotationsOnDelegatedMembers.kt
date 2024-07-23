// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ISSUE: KT-64466

annotation class Ann

interface Base {
    @Ann
    fun func()

    @Ann
    val prop: Int

    var propWithAccessors: Int
        @Ann get
        @Ann set
}

class Delegated(b: Base) : Base by b
