var x: Any = 2

class Test {
    init {
        val (type, entityName) = when {
            x is Int -> Int::class.java to "Int"
            <caret>x is String -> String::class.java to "String"
            else -> null to null
        }
    }
}
