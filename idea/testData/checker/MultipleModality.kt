sealed <warning descr="[REDUNDANT_MODIFIER] Modifier 'abstract' is redundant because 'sealed' is present">abstract</warning> class First

<warning descr="[REDUNDANT_MODIFIER] Modifier 'abstract' is redundant because 'sealed' is present">abstract</warning> sealed class Second

abstract class Base {
    abstract <warning descr="[REDUNDANT_MODIFIER] Modifier 'open' is redundant because 'abstract' is present">open</warning> fun foo()

    <warning descr="[REDUNDANT_MODIFIER] Modifier 'open' is redundant because 'abstract' is present">open</warning> abstract val name: String
}

open class Derived : Base() {
    override <warning descr="[REDUNDANT_MODIFIER] Modifier 'open' is redundant because 'override' is present">open</warning> fun foo() {}

    override final val name = ""
}
