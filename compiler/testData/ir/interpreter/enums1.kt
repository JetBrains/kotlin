import kotlin.*
import kotlin.collections.*

@CompileTimeCalculation
enum class EnumClass {
    VALUE1, VALUE2
}

const val a = <!EVALUATED: `VALUE1`!>EnumClass.VALUE1.name<!>
const val b = <!EVALUATED: `2`!>EnumClass.values().size<!>
const val c = <!EVALUATED: `0`!>EnumClass.valueOf("VALUE1").ordinal<!>
const val d = <!WAS_NOT_EVALUATED: `
Exception java.lang.IllegalArgumentException: No enum constant EnumClass.VALUE3
	at Enums1Kt.EnumClass.valueOf(enums1.kt)
	at Enums1Kt.<clinit>(enums1.kt:12)`!>EnumClass.valueOf("VALUE3").ordinal<!>

const val e1 = <!EVALUATED: `true`!>EnumClass.VALUE1.hashCode().let { it is Int && it > 0 && it == EnumClass.VALUE1.hashCode() }<!>
const val e2 = <!EVALUATED: `VALUE1`!>EnumClass.VALUE1.toString()<!>
const val e3 = <!EVALUATED: `true`!>EnumClass.VALUE1 == EnumClass.VALUE1<!>
const val e4 = <!EVALUATED: `false`!>EnumClass.VALUE1 == EnumClass.VALUE2<!>

const val f1 = <!EVALUATED: `2`!>enumValues<EnumClass>().size<!>
const val f2 = <!EVALUATED: `VALUE1`!>enumValueOf<EnumClass>("VALUE1").name<!>

const val j1 = <!EVALUATED: `VALUE1, VALUE2`!>enumValues<EnumClass>().joinToString { it.name }<!>

@CompileTimeCalculation
fun getEnumValue(flag: Boolean): EnumClass {
	return if (flag) EnumClass.VALUE1 else EnumClass.VALUE2
}

const val conditional1 = <!EVALUATED: `VALUE1`!>getEnumValue(true).name<!>
const val conditional2 = <!EVALUATED: `VALUE2`!>getEnumValue(false).name<!>
