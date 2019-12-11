// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
//KT-2438 Prohibit inner classes with the same name

package kt2438

class B {
    class C
    class C
}



class A {
    class B
    
    companion object {
        class B
        class B
    }
    
    class B
}
