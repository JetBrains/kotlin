// MODULE: extendedModule

// MODULE: dependency2

// MODULE: main(extendedModule, dependency2)()()
import generated.*

fun main() {
    val a = GeneratedClass2()
    a.gener<caret>atedClassMember2()
}
