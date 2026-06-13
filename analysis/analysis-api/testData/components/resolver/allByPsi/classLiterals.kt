typealias MyAlias = String

inline fun <reified T> main() {
   T::class
   MyAlias::class
   String::class
   kotlin.Int.Companion::class
}
