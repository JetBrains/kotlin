// "Create function 'invoke' from usage" "true"

class A<T>(val n: T)

fun test(): A<String> {
    return <caret>1(2, "2")
}