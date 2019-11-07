// IS_APPLICABLE: false
@Ann("""foo
bar
baz"""<caret>)
class Test

annotation class Ann(val s: String)