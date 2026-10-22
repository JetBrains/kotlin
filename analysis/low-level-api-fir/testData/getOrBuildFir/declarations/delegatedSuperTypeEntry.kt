// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDelegatedSuperTypeEntry
interface MyInterface

class MyClass(i: MyInterface) : <expr>MyInterface by i</expr>
