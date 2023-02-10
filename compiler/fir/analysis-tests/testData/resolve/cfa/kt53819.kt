// ISSUE: KT-53819
fun bar() {
    class Foo {
        private var _x: Int

        constructor(x: Int) {
            this._x = x
        }

        val x: Int
            get() = _x
    }
}

