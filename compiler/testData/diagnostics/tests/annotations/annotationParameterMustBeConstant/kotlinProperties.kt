annotation class Ann(vararg val i: Int)

@Ann(
        <!ANNOTATION_PARAMETER_MUST_BE_CONST!>i1<!>,
        i2,
        <!ANNOTATION_PARAMETER_MUST_BE_CONST!>i3<!>,
        i4,
        <!ANNOTATION_PARAMETER_MUST_BE_CONST!>i5<!>,
        <!ANNOTATION_PARAMETER_MUST_BE_CONST!>i6<!>
)
class Test

var i1 = 1  // var
const val i2 = 1  // val
val i3 = i1 // val with var in initializer
const val i4 = i2 // val with val in initializer
var i5 = i1 // var with var in initializer
var i6 = i2 // var with val in initializer
