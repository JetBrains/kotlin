// "Add missing 'constructor' keyword in whole project" "true"
// ERROR: Use 'constructor' keyword after modifiers of primary constructor
// ERROR: Use 'constructor' keyword after modifiers of primary constructor
// ERROR: Use 'constructor' keyword after modifiers of primary constructor

annotation class Ann(val x: Int = 1)

class A @Ann(1)private constructor(x: Int) {
    inner class B() // do not insert here
    inner class C        protected constructor() {
        fun foo() {
            class Local private constructor()
        }
    }
}
