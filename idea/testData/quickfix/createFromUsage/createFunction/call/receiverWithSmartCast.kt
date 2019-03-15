// "Create member function 'SomeObj.doSomething'" "true"
class SomeObj { }

fun doSomething(p: Any): List<Number>{
    if (p is SomeObj){
        p.<caret>doSomething()

    }
    return emptyList()
}