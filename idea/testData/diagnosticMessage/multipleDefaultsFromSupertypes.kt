// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE

trait C {
    fun foo(x: Int = 42)
}

trait D {
    fun foo(x: Int = 239) {}
}

class multipleDefaultsFromSupertypes : C, D
