// TARGET_BACKEND: JVM
// WITH_STDLIB

abstract class Base {
    protected abstract fun getChart(context: CharSequence): String

    @get:JvmName("getChartHelper")
    public val CharSequence.chart get() = getChart(this)
}

abstract class Derived1 : Base()

class Derived2 : Derived1() {
    override fun getChart(context: CharSequence): String {
        return context.toString()
    }
}

fun box(): String {
    return with(Derived2()) {
        "OK".chart
    }
}
