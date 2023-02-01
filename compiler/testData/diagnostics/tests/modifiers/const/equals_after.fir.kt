// !LANGUAGE: +IntrinsicConstEvaluation

const val equalsBoolean1 = true.equals(true)
const val equalsBoolean2 = false != true
const val equalsBoolean3 = false.equals(1)
const val equalsBoolean4 = <!EQUALITY_NOT_APPLICABLE!>false == 1<!>

const val equalsChar1 = '1'.equals('2')
const val equalsChar2 = '2' == '2'
const val equalsChar3 = '1'.equals(1)
const val equalsChar4 = <!EQUALITY_NOT_APPLICABLE!>'1' == 1<!>

const val equalsByte1 = 1.toByte().equals(2.toByte())
const val equalsByte2 = 2.toByte() == 2.toByte()
const val equalsByte3 = 1.toByte().equals("1")
const val equalsByte4 = <!EQUALITY_NOT_APPLICABLE!>1.toByte() == "1"<!>

const val equalsShort1 = 1.toShort().equals(2.toShort())
const val equalsShort2 = 2.toShort() == 2.toShort()
const val equalsShort3 = 1.toShort().equals("1")
const val equalsShort4 = <!EQUALITY_NOT_APPLICABLE!>1.toShort() == "1"<!>

const val equalsInt1 = 1.equals(2)
const val equalsInt2 = 2 == 2
const val equalsInt3 = 1.equals("1")
const val equalsInt4 = <!EQUALITY_NOT_APPLICABLE!>1 == "1"<!>

const val equalsLong1 = 1L.equals(2L)
const val equalsLong2 = 2L == 2L
const val equalsLong3 = 1L.equals("1")
const val equalsLong4 = <!EQUALITY_NOT_APPLICABLE!>1L == "1"<!>

const val equalsFloat1 = 1.0f.equals(2.0f)
const val equalsFloat2 = 2.0f == 2.0f
const val equalsFloat3 = 1.0f.equals("1")
const val equalsFloat4 = <!EQUALITY_NOT_APPLICABLE!>1.0f == "1"<!>

const val equalsDoable1 = 1.0.equals(2.0)
const val equalsDoable2 = 2.0 == 2.0
const val equalsDoable3 = 1.0.equals("1")
const val equalsDoable4 = <!EQUALITY_NOT_APPLICABLE!>1.0 == "1"<!>

const val equalsString1 = "someStr".equals("123")
const val equalsString2 = "someStr" == "otherStr"
const val equalsString3 = "someStr".equals(1)
const val equalsString4 = <!EQUALITY_NOT_APPLICABLE!>"someStr" == 1<!>
