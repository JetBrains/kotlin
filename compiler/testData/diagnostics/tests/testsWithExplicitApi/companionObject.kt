// FIR_IDENTICAL
// SKIP_TXT

public class Bar {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>companion object<!> {}
}

public class Bar2 {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>companion object MyCompanion<!> {}
}

public class Bar3 {
    /**
     * Nested object KDoc
     */
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>object NestedObject<!> {}
}

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>data class FooData2<!>(val i: Int, val s: String) {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>object NestedObject<!> {}
}