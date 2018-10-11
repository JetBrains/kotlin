// IS_APPLICABLE: false
val f: (Any) -> Boolean = <caret>{ a -> a::class.isInstance(a) }