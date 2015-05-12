interface First {
    fun foo123()
    fun foo12_()
    fun foo1_3()
    fun foo_23()

    val bar123: String
    val bar12_: String
    val bar1_3: String
    val bar_23: String

    fun foo1__()
    fun foo_2_()
    fun foo__3()

    val bar1__: String
    val bar_2_: String
    val bar__3: String

    fun foo___()
    val bar___: String
}

abstract class A1 : First {
    override fun foo123() {}
    override fun foo12_() {}
    override fun foo1_3() {}

    override val bar123 = "test"
    override val bar12_ = "test"
    override val bar1_3 = "test"

    override fun foo1__() {}
    override val bar1__ = "test"
}

abstract class A2 : A1() {
    override fun foo123() {}
    override fun foo12_() {}
    override fun foo_23() {}

    override val bar123 = "test"
    override val bar12_ = "test"
    override val bar_23 = "test"

    override fun foo_2_() {}
    override val bar_2_ = "test"
}

open class A3 : A2() {
    override fun foo123() {}
    override fun foo1_3() {}
    override fun foo_23() {}

    override val bar123 = "test"
    override val bar1_3 = "test"
    override val bar_23 = "test"

    override fun foo__3() {}
    override val bar__3 = "test"
}

