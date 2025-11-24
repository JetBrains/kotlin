import kotlin.*
import kotlin.collections.*

@CompileTimeCalculation
enum class EnumClass {
    VALUE1, VALUE2
}

const val a = <!EVALUATED: `VALUE1`!>EnumClass.VALUE1.name<!>
const val b = <!EVALUATED: `0`!>EnumClass.valueOf("VALUE1").ordinal<!>
const val c = <!WAS_NOT_EVALUATED: `
Exception java.lang.IllegalArgumentException: No enum constant EnumClass.VALUE3
	at Enums1Kt.EnumClass.valueOf(enums1.kt)
	at Enums1Kt.<clinit>(enums1.kt:11)`!>EnumClass.valueOf("VALUE3").ordinal<!>

const val e1 = <!EVALUATED: `VALUE1`!>EnumClass.VALUE1.toString()<!>
const val e2 = <!EVALUATED: `true`!>EnumClass.VALUE1 == EnumClass.VALUE1<!>
const val e3 = <!EVALUATED: `false`!>EnumClass.VALUE1 == EnumClass.VALUE2<!>

const val f1 = <!EVALUATED: `VALUE1`!>enumValueOf<EnumClass>("VALUE1").name<!>


@CompileTimeCalculation
fun getEnumValue(flag: Boolean): EnumClass {
	return if (flag) EnumClass.VALUE1 else EnumClass.VALUE2
}

const val conditional1 = <!EVALUATED: `VALUE1`!>getEnumValue(true).name<!>
const val conditional2 = <!EVALUATED: `VALUE2`!>getEnumValue(false).name<!>
