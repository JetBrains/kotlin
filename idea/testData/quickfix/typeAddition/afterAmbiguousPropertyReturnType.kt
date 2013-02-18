// "Specify type explicitly" "true"
package a

trait A {}

trait B {}

class C {
    val property: B<caret> = object : B, A {}
}