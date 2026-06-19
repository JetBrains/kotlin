// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: JVM_IR, NATIVE

// This test is essentially the same one as initializersInheritanceMultipleSupertypes,
// but with different super type kinds order (interface, class, interface)

var initOrder = ""

open class Bar {
    companion {
        val barProp1: String = run { initOrder += "BAR1 "; "bar1" }
        val barProp2: String = run { initOrder += "BAR2 "; "bar2" }
    }
}

interface First {
    companion {
        val firstProp: String = run { initOrder += "IF1 "; "first" }
    }

    // A non-abstract (default) member. Per KEEP §3.3, initializing an implementing
    // classifier must also trigger this interface's companion block initialization.
    fun first(): String = "first-default"
}

interface Second {
    companion {
        val secondProp: String = run { initOrder += "IF2 "; "second" }
    }

    // A non-abstract (default) member, so this interface participates in init too.
    fun second(): String = "second-default"
}

class Foo : Second, Bar(), First {
    companion {
        val fooProp1: String = run { initOrder += "FOO1 "; "foo1" }
        val fooProp2: String = run { initOrder += "FOO2 "; "foo2" }
    }
}

fun box(): String {
    // Accessing a companion member of Foo triggers its initialization, which per
    // KEEP §3.3 first initializes the companion blocks of all supertypes that have
    // non-abstract members: the superclass `Bar` and the superinterfaces `First`
    // and `Second`. Following JVM semantics, the superclass is initialized first,
    // then superinterfaces in declaration order, and finally `Foo` itself. Within
    // each class, §3.2 program order is respected.
    val foo1 = Foo.fooProp1

    // All supertype properties must be initialized.
    if (Second.secondProp != "second") return "FAIL: secondProp=${Second.secondProp}"
    if (Bar.barProp1 != "bar1") return "FAIL: barProp1=${Bar.barProp1}"
    if (Bar.barProp2 != "bar2") return "FAIL: barProp2=${Bar.barProp2}"
    if (First.firstProp != "first") return "FAIL: firstProp=${First.firstProp}"

    // Foo's own properties must be initialized.
    if (foo1 != "foo1") return "FAIL: fooProp1=$foo1"
    if (Foo.fooProp2 != "foo2") return "FAIL: fooProp2=${Foo.fooProp2}"

    // Order: superclass, then superinterfaces (declaration order), then Foo itself.
    if (initOrder != "IF2 BAR1 BAR2 IF1 FOO1 FOO2 ") return "FAIL: initOrder=$initOrder"

    // Accessing companion members again must not re-run any initializer.
    if (Foo.fooProp2 != "foo2") return "FAIL: fooProp2 (second access)=${Foo.fooProp2}"
    if (initOrder != "IF2 BAR1 BAR2 IF1 FOO1 FOO2 ") return "FAIL: initOrder after re-access=$initOrder"

    return "OK"
}
