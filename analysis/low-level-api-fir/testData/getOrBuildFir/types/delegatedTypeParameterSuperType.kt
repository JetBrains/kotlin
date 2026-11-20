// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
interface MyInterface

class MyClass<T : MyInterface>(val i: T) {
    inner class Inner : <expr>T</expr> by i
}
