// !DIAGNOSTICS: -UNUSED_PARAMETER

const val intConst = 1
const val longConst: Long = 1
const val boolConst = true
const val stringConst = "empty"

enum class MyEnum { A }

const val enumConst: MyEnum = MyEnum.A
const val arrayConst: Array<String> = arrayOf("1")
const val intArrayConst: IntArray = intArrayOf()

const val unresolvedConst1 = <!UNRESOLVED_REFERENCE!>Unresolved<!>
const var unresolvedConst2 = <!UNRESOLVED_REFERENCE!>Unresolved<!>
const val unresolvedConst3 = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD, UNRESOLVED_REFERENCE!>Unresolved<!>
get() = 10
