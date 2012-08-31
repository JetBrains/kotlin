open data class A(val x: Int, val y: String)

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class B(val z: String) : A(42, "")
