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

const val a1 = <!EVALUATED: `0`!>Empty.values().size<!>
const val a2 = <!EVALUATED: `0`!>enumValues<Empty>().size<!>

const val b1 = <!EVALUATED: `BLACK`!>Color.BLACK.name<!>
const val b2 = <!EVALUATED: `0x000000`!>Color.BLACK.getColorAsString()<!>
const val b3 = <!EVALUATED: `0xFF0000`!>Color.RED.getColorAsString()<!>

const val c1 = <!EVALUATED: `0`!>Color.BLACK.getColorAsInt()<!>
const val c2 = <!EVALUATED: `16711680`!>Color.RED.getColorAsInt()<!>

@CompileTimeCalculation
enum class EnumWithoutPrimary {
    X(), Y(10);

    val someProp: Int

    constructor() : this(0) {}
    constructor(value: Int) {
        someProp = value
    }
}

const val d1 = <!EVALUATED: `0`!>EnumWithoutPrimary.X.someProp<!>
const val d2 = <!EVALUATED: `10`!>EnumWithoutPrimary.Y.someProp<!>
