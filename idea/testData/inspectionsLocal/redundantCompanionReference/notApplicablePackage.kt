// PROBLEM: none
package my.sample

class Class {
    fun test() {
        my.sample.Class<caret>.say()
    }

    companion object Class {
        fun say() {}
    }
}