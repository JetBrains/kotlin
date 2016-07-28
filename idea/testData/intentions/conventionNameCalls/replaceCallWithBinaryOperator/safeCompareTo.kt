// IS_APPLICABLE: false
// ERROR: Infix call corresponds to a dot-qualified call 'nullable?.compareTo(1).compareTo(0)' which is not allowed on a nullable receiver 'nullable?.compareTo(1)'. Use '?.'-qualified call instead

val nullable: Int? = null
val x = nullable?.compareTo<caret>(1) >= 0