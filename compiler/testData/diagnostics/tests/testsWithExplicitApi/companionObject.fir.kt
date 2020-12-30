// SKIP_TXT

public class Bar {
    companion object {}
}

public class Bar2 {
    companion object MyCompanion {}
}

public class Bar3 {
    /**
     * Nested object KDoc
     */
    object NestedObject {}
}

data class FooData2(val i: Int, val s: String) {
    object NestedObject {}
}