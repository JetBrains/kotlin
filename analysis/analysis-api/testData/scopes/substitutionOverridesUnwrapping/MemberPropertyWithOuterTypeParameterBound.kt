// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
package test

interface ClassA
interface ClassB

interface MyInterface<T> {
    val <Own : ClassA> Own.withOwnGeneric_InterfaceWithValBase: ClassA
    val <Own : T> Own.withOwnAndOuterGenericAsTypeBound_InterfaceWithValBase: ClassA
}

abstract class <caret>Inheritor : MyInterface<ClassB>
