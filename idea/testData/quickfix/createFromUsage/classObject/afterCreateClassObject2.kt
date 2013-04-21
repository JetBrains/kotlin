// "Create class object from usage" "true"
// ERROR: Expression 'T' of type '<class-object-for-T>' cannot be invoked as a function
trait T
{
    class object {
    }
}
fun foo() {
    val x = T()
}
