// Mostly tested as intention. Here we just test shouldSuggestToConvert()

// Should suggest to convert
class TheirWrapper(val x: Int)
val x = { y: Int -> TheirWrapper(y) }

// Should suggest to convert despite of too long reference
fun foo(arg: TheirWrapper, convert: (TheirWrapper) -> String) = convert(arg)
val y = foo(TheirWrapper(42)) { it.toString() }

// Also should suggest to convert, but only call should be highlighted
fun bar(arg: Int, convert: (Int) -> TheirWrapper) = convert(arg)
val z = bar(42) {
    TheirWrapper(it)
}