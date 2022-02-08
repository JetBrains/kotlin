import kotlin.*
import kotlin.collections.*

@CompileTimeCalculation
enum class EnumClass {
    VALUE1, VALUE2
}

const val a = EnumClass.VALUE1.<!EVALUATED: `VALUE1`!>name<!>
const val b = EnumClass.values().<!EVALUATED: `2`!>size<!>
const val c = EnumClass.valueOf("VALUE1").<!EVALUATED: `0`!>ordinal<!>
const val d = EnumClass.valueOf("VALUE3").<!WAS_NOT_EVALUATED: `
Exception java.lang.IllegalArgumentException: No enum constant EnumClass.VALUE3
	at Enums1Kt.EnumClass.valueOf(enums1.kt)
	at Enums1Kt.<clinit>(enums1.kt:12)`!>ordinal<!>

const val e1 = EnumClass.VALUE1.hashCode().<!EVALUATED: `true`!>let { it is Int && it > 0 && it == EnumClass.VALUE1.hashCode() }<!>
const val e2 = EnumClass.VALUE1.<!EVALUATED: `VALUE1`!>toString()<!>
const val e3 = <!EVALUATED: `true`!>EnumClass.VALUE1 == EnumClass.VALUE1<!>
const val e4 = <!EVALUATED: `false`!>EnumClass.VALUE1 == EnumClass.VALUE2<!>

const val f1 = enumValues<EnumClass>().<!EVALUATED: `2`!>size<!>
const val f2 = enumValueOf<EnumClass>("VALUE1").<!EVALUATED: `VALUE1`!>name<!>

const val j1 = enumValues<EnumClass>().<!EVALUATED: `VALUE1, VALUE2`!>joinToString { it.name }<!>
