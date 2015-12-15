// !DIAGNOSTICS: -UNUSED_PARAMETER

const val intConst = 1
const val longConst: Long = 1
const val boolConst = true
const val stringConst = "empty"

enum class MyEnum { A }

<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val enumConst: MyEnum = MyEnum.A
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val arrayConst: Array<String> = arrayOf("1")
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val intArrayConst: IntArray = intArrayOf()
