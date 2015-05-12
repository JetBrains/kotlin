// "Specify return type explicitly" "true"
package a

interface A {}

interface B {}

class C {
    fun <caret>property() = object : B, A {}
}