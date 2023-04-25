// !LANGUAGE: +IntrinsicConstEvaluation

class SomeClassWithName(val property: Int) {
    val anotherProperty: String = ""

    fun foo() {}
    fun bar(a: Int, b: Double): String = ""
}

const val className = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>::SomeClassWithName.name<!>
const val propName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>SomeClassWithName::property.name<!>
const val anotherPropName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>SomeClassWithName::anotherProperty.name<!>
const val fooName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>SomeClassWithName::foo.name<!>
const val barName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>SomeClassWithName::bar.name<!>

const val stringClassName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>::String.name<!>
const val lengthPropName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>String::length.name<!>

const val errorAccess = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>SomeClassWithName(1)::property.name<!>
const val errorPlus = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"" + SomeClassWithName(1)::property<!>
