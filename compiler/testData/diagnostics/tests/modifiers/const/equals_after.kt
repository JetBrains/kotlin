// !LANGUAGE: +IntrinsicConstEvaluation

const val equalsBoolean1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>true.equals(true)<!>
const val equalsBoolean2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>false != true<!>
const val equalsBoolean3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>false.equals(1)<!>
const val equalsBoolean4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EQUALITY_NOT_APPLICABLE!>false == 1<!>

const val equalsChar1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>'1'.equals('2')<!>
const val equalsChar2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>'2' == '2'<!>
const val equalsChar3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>'1'.equals(1)<!>
const val equalsChar4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EQUALITY_NOT_APPLICABLE!>'1' == 1<!>

const val equalsByte1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.toByte().equals(2.toByte())<!>
const val equalsByte2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2.toByte() == 2.toByte()<!>
const val equalsByte3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.toByte().equals("1")<!>
const val equalsByte4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EQUALITY_NOT_APPLICABLE!>1.toByte() == "1"<!>

const val equalsShort1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.toShort().equals(2.toShort())<!>
const val equalsShort2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2.toShort() == 2.toShort()<!>
const val equalsShort3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.toShort().equals("1")<!>
const val equalsShort4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EQUALITY_NOT_APPLICABLE!>1.toShort() == "1"<!>

const val equalsInt1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.equals(2)<!>
const val equalsInt2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2 == 2<!>
const val equalsInt3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.equals("1")<!>
const val equalsInt4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EQUALITY_NOT_APPLICABLE!>1 == "1"<!>

const val equalsLong1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1L.equals(2L)<!>
const val equalsLong2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2L == 2L<!>
const val equalsLong3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1L.equals("1")<!>
const val equalsLong4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EQUALITY_NOT_APPLICABLE!>1L == "1"<!>

const val equalsFloat1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.0f.equals(2.0f)<!>
const val equalsFloat2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2.0f == 2.0f<!>
const val equalsFloat3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.0f.equals("1")<!>
const val equalsFloat4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EQUALITY_NOT_APPLICABLE!>1.0f == "1"<!>

const val equalsDoable1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.0.equals(2.0)<!>
const val equalsDoable2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2.0 == 2.0<!>
const val equalsDoable3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.0.equals("1")<!>
const val equalsDoable4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EQUALITY_NOT_APPLICABLE!>1.0 == "1"<!>

const val equalsString1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"someStr".equals("123")<!>
const val equalsString2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"someStr" == "otherStr"<!>
const val equalsString3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"someStr".equals(1)<!>
const val equalsString4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, EQUALITY_NOT_APPLICABLE!>"someStr" == 1<!>

const val TRUE = true
const val STR = "str"

const val equalsWithNull1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, SENSELESS_COMPARISON!>1 == null<!>
const val equalsWithNull2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, SENSELESS_COMPARISON!>null == null<!>
const val equalsWithNull3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, SENSELESS_COMPARISON!>TRUE == null<!>
const val equalsWithNull4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, SENSELESS_COMPARISON!>STR == null<!>
