// "Specify type explicitly" "true"
package a

trait A {}

trait B {}

class C {
    val <caret>property = object : B, A {}
}