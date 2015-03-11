package test

trait Bbb<P>

class ClassObjectExtendsTraitWithTP {
    default object : Bbb<String> {
    }
}
