// "Make all upper bounds non-nullable" "true"
class Foo<P, Q : Number?> {
    inner class Bar<R, S : Q> where R : P??? {
        fun boo<T : R, U> () where T : S?, U : Number? {
            var y: T?<caret> = null
        }
    }
}