import testData.libraries.*

fun foo(a : ClassWithAbstractAndOpenMembers) {
    a.abstractVar = "v"
    println(a.abstractVar)
}

