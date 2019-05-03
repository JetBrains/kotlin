// PROBLEM: none
package my.simple.name

class Inner {
    fun a() {
        this<caret>.say()
    }

    fun say() {}
}
