import kotlin.Self

abstract class AbstractClass {

}

val abstractClassObject = <!SELF_TYPE_INAPPLICABLE_TARGET!>@Self<!> object : AbstractClass() {

}

val obj = <!SELF_TYPE_INAPPLICABLE_TARGET!>@Self<!> object {

}