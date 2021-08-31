@CompileTimeCalculation
enum class Empty

@CompileTimeCalculation
enum class Color(val rgb: Int) {
    BLACK() { override fun getColorAsString() = "0x000000" },
    RED(0xFF0000) { override fun getColorAsString() = "0xFF0000" },
    GREEN(0x00FF00) { override fun getColorAsString() = "0x00FF00" },
    BLUE(0x0000FF) { override fun getColorAsString() = "0x0000FF" };

    constructor() : this(0x000000) {}

    abstract fun getColorAsString(): String

    fun getColorAsInt(): Int = rgb
}

const val a1 = Empty.values().<!EVALUATED: `0`!>size<!>
const val a2 = enumValues<Empty>().<!EVALUATED: `0`!>size<!>

const val b1 = Color.BLACK.<!EVALUATED: `BLACK`!>name<!>
const val b2 = Color.BLACK.<!EVALUATED: `0x000000`!>getColorAsString()<!>
const val b3 = Color.RED.<!EVALUATED: `0xFF0000`!>getColorAsString()<!>

const val c1 = Color.BLACK.<!EVALUATED: `0`!>getColorAsInt()<!>
const val c2 = Color.RED.<!EVALUATED: `16711680`!>getColorAsInt()<!>

@CompileTimeCalculation
enum class EnumWithoutPrimary {
    X(), Y(10);

    val someProp: Int

    constructor() : this(0) {}
    constructor(value: Int) {
        someProp = value
    }
}

const val d1 = EnumWithoutPrimary.X.<!EVALUATED: `0`!>someProp<!>
const val d2 = EnumWithoutPrimary.Y.<!EVALUATED: `10`!>someProp<!>
