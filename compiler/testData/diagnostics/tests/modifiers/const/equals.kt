// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: IntrinsicConstEvaluation

const val equalsBoolean1 = true.equals(true)
const val equalsBoolean2 = false != true
const val equalsBoolean3 = false.equals(1)
const val equalsBoolean4 = <!EQUALITY_NOT_APPLICABLE!>false == 1<!>

const val equalsChar1 = '1'.equals('2')
const val equalsChar2 = '2' == '2'
const val equalsChar3 = '1'.equals(1)
const val equalsChar4 = <!EQUALITY_NOT_APPLICABLE!>'1' == 1<!>
const val equalsChar5 = '2' != '1'
const val equalsChar6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>'2' === '2'<!>
const val equalsChar7 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>'2' !== '1'<!>

const val equalsByte1 = 1.toByte().equals(2.toByte())
const val equalsByte2 = 2.toByte() == 2.toByte()
const val equalsByte3 = 1.toByte().equals("1")
const val equalsByte4 = <!EQUALITY_NOT_APPLICABLE!>1.toByte() == "1"<!>
const val equalsByte5 = 2.toByte() != 1.toByte()
const val equalsByte6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>2.toByte() === 1.toByte()<!>
const val equalsByte7 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>2.toByte() !== 1.toByte()<!>

const val equalsShort1 = 1.toShort().equals(2.toShort())
const val equalsShort2 = 2.toShort() == 2.toShort()
const val equalsShort3 = 1.toShort().equals("1")
const val equalsShort4 = <!EQUALITY_NOT_APPLICABLE!>1.toShort() == "1"<!>
const val equalsShort5 = 2.toShort() != 1.toShort()
const val equalsShort6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>2.toShort() === 1.toShort()<!>
const val equalsShort7 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>2.toShort() !== 1.toShort()<!>

const val equalsInt1 = 1.equals(2)
const val equalsInt2 = 2 == 2
const val equalsInt3 = 1.equals("1")
const val equalsInt4 = <!EQUALITY_NOT_APPLICABLE!>1 == "1"<!>
const val equalsInt5 = 2 != 1
const val equalsInt6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>2 === 2<!>
const val equalsInt7 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>2 !== 1<!>

const val equalsLong1 = 1L.equals(2L)
const val equalsLong2 = 2L == 2L
const val equalsLong3 = 1L.equals("1")
const val equalsLong4 = <!EQUALITY_NOT_APPLICABLE!>1L == "1"<!>
const val equalsLong5 = 2L != 1L
const val equalsLong6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>2L === 2L<!>
const val equalsLong7 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>2L !== 1L<!>

const val equalsFloat1 = 1.0f.equals(2.0f)
const val equalsFloat2 = 2.0f == 2.0f
const val equalsFloat3 = 1.0f.equals("1")
const val equalsFloat4 = <!EQUALITY_NOT_APPLICABLE!>1.0f == "1"<!>
const val equalsFloat5 = 2.0f != 1.0f
const val equalsFloat6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>2.0f === 2.0f<!>
const val equalsFloat7 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DEPRECATED_IDENTITY_EQUALS!>2.0f !== 1.0f<!>

const val equalsDoable1 = 1.0.equals(2.0)
const val equalsDoable2 = 2.0 == 2.0
const val equalsDoable3 = 1.0.equals("1")
const val equalsDoable4 = <!EQUALITY_NOT_APPLICABLE!>1.0 == "1"<!>
const val equalsDoable5 = <!EQUALITY_NOT_APPLICABLE!>2.0 != "1"<!>
const val equalsDoable6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, FORBIDDEN_IDENTITY_EQUALS!>2 === "1"<!>
const val equalsDoable7 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, FORBIDDEN_IDENTITY_EQUALS!>2 !== "1"<!>

const val equalsString1 = "someStr".equals("123")
const val equalsString2 = "someStr" == "otherStr"
const val equalsString3 = "someStr".equals(1)
const val equalsString4 = <!EQUALITY_NOT_APPLICABLE!>"someStr" == 1<!>
const val equalsString5 = "someStr" != "123"
const val equalsString6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"someStr" === "otherStr"<!>
const val equalsString7 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"someStr" !== "123"<!>

const val TRUE = true
const val STR = "str"

const val equalsWithNull1 = <!SENSELESS_COMPARISON!>1 == <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>null<!><!>
const val equalsWithNull2 = <!SENSELESS_COMPARISON!><!CONST_VAL_WITH_NON_CONST_INITIALIZER!>null<!> == null<!>
const val equalsWithNull3 = <!SENSELESS_COMPARISON!>TRUE == <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>null<!><!>
const val equalsWithNull4 = <!SENSELESS_COMPARISON!>STR == <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>null<!><!>

/* GENERATED_FIR_TAGS: const, equalityExpression, integerLiteral, propertyDeclaration, stringLiteral */

