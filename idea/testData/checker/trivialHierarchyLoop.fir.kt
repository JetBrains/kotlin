class A : <error descr="[OTHER_ERROR] Unknown (other) error">A</error>() {}

val x : Int = <error descr="[INITIALIZER_TYPE_MISMATCH] Initializer type mismatch: expected kotlin/Int, actual A">A()</error>
