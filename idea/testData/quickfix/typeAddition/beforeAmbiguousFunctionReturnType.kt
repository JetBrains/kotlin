// "Specify return type explicitly" "true"
package a

trait A {}

trait B {}

class C {
    fun <caret>property() = object : B, A {}
}