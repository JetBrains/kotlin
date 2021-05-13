class A() {
    var x: Int = 0
        get() = "s"
        set(value: <error descr="[WRONG_SETTER_PARAMETER_TYPE] Setter parameter type must be equal to the type of the property, i.e. 'kotlin/Int'">String</error>) {
            field = <error descr="[ASSIGNMENT_TYPE_MISMATCH] Assignment type mismatch: actual type is kotlin/String but kotlin/Int was expected">value</error>
        }
    val y: Int
        get(): <error descr="[WRONG_GETTER_RETURN_TYPE] Getter return type must be equal to the type of the property, i.e. 'kotlin/Int'">String</error> = "s"
    val z: Int
        get() {
            return "s"
        }

    var a: Any = 1
        set(v: <error descr="[WRONG_SETTER_PARAMETER_TYPE] Setter parameter type must be equal to the type of the property, i.e. 'kotlin/Any'">String</error>) {
            field = v
        }
    val b: Int
        get(): <error descr="[WRONG_GETTER_RETURN_TYPE] Getter return type must be equal to the type of the property, i.e. 'kotlin/Int'">Any</error> = "s"
    val c: Int
        get() {
            return 1
        }
    val d = 1
        get() {
            return field
        }
    val e = 1
        get(): <error descr="[WRONG_GETTER_RETURN_TYPE] Getter return type must be equal to the type of the property, i.e. 'kotlin/Int'">String</error> {
            return field
        }

}
