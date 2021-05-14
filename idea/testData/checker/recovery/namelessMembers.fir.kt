class C {
    <error descr="[FUNCTION_DECLARATION_WITH_NO_NAME] Function declaration must have a name">fun ()</error> {

    }

    val<error descr="Expecting property name or receiver type"> </error>: Int = 1

    class<error descr="Name expected"> </error>{}

    <error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'kotlin/Enum<C.<no name provided>>'">enum class<error descr="Name expected"> </error>{}</error>
}

class C1<<error descr="Type parameter name expected">in</error>> {}

class C2(<error descr="[VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION] A type annotation is required on a value parameter">val</error><error descr="Parameter name expected">)</error> {}
