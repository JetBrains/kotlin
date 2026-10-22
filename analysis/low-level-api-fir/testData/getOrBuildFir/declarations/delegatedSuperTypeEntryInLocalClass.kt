// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDelegatedSuperTypeEntry
interface MyInterface

fun foo(i: MyInterface) {
    class Local : <expr>MyInterface by i</expr>
}
