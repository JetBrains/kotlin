// "Specify return type explicitly" "true"
package a

trait A {}

trait B {}

class C {
    fun property(): B<caret> = object : B, A {}
}