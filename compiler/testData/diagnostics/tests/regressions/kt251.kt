// RUN_PIPELINE_TILL: FRONTEND
class A() {
    var x: Int = 0
        get() = <!TYPE_MISMATCH!>"s"<!>
        set(value: <!WRONG_SETTER_PARAMETER_TYPE!>String<!>) {
            field = <!TYPE_MISMATCH!>value<!>
        }
    val y: Int
        get(): <!WRONG_GETTER_RETURN_TYPE("Int; String")!>String<!> = "s"
    val z: Int
        get() {
            return <!TYPE_MISMATCH!>"s"<!>
        }

    var a: Any = 1
        set(v: <!WRONG_SETTER_PARAMETER_TYPE!>String<!>) {
            field = v
        }
    val b: Int
        get(): <!WRONG_GETTER_RETURN_TYPE!>Any<!> = "s"
    val c: Int
        get() {
            return 1
        }
    val d = 1
        get() {
            return field
        }
    val e = 1
        get(): <!WRONG_GETTER_RETURN_TYPE!>String<!> {
            return <!TYPE_MISMATCH!>field<!>
        }

}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, getter, integerLiteral, primaryConstructor, propertyDeclaration,
setter, stringLiteral */
