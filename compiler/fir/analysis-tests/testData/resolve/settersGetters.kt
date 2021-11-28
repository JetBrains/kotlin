class SomeClass {

    var foo: Int = 0
        set(value: <!WRONG_SETTER_PARAMETER_TYPE!>String<!>){
            field = <!ASSIGNMENT_TYPE_MISMATCH!>value<!>
        }

}
