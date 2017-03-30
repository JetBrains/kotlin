// ERROR: Cannot perform refactoring.\nInline Function is not supported for functions with multiple return statements.

enum class EnumWithConstructor(s: String, s2: String) {
    Test("1"), Rest("2", "b");

    <caret>constructor(s: String): this(s, "")
}