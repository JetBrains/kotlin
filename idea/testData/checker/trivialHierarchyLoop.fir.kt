class A : <error descr="[OTHER_ERROR] Unknown (other) error">A</error>() {}

val x : Int = <error descr="[INITIALIZER_TYPE_MISMATCH] Initializer type mismatch: expected kotlin/Int, actual A"><error descr="[TYPE_MISMATCH] Type mismatch: inferred type is A but kotlin/Int was expected">A()</error></error>
