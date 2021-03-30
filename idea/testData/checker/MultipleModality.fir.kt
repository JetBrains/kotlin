sealed <error descr="[REDUNDANT_MODIFIER] Modifier 'abstract' is redundant because 'sealed' is present">abstract</error> class First

<error descr="[REDUNDANT_MODIFIER] Modifier 'abstract' is redundant because 'sealed' is present">abstract</error> sealed class Second

abstract class Base {
    abstract <error descr="[REDUNDANT_MODIFIER] Modifier 'open' is redundant because 'abstract' is present">open</error> fun foo()

    <error descr="[REDUNDANT_MODIFIER] Modifier 'open' is redundant because 'abstract' is present">open</error> abstract val name: String
}
