// !LANGUAGE: +IntrinsicConstEvaluation

class SomeClassWithName(val property: Int) {
    val anotherProperty: String = ""

    fun foo() {}
    fun bar(a: Int, b: Double): String = ""
}

const val className = ::SomeClassWithName.name
const val propName = SomeClassWithName::property.name
const val anotherPropName = SomeClassWithName::anotherProperty.name
const val fooName = SomeClassWithName::foo.name
const val barName = SomeClassWithName::bar.name

const val stringClassName = ::String.name
const val lengthPropName = String::length.name

const val errorAccess = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>SomeClassWithName(1)::property.name<!>
const val errorPlus = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"" + SomeClassWithName(1)::property<!>
