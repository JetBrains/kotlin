import java.lang.invoke.MethodHandles

fun test(short: Short, long: Long, float: Float,
         string: String, char: Char, collection: Collection<Any>,
         double: Double, boolean: Boolean, list: List<Any>,
         int: Int, byte: Byte, enumType: MyEnum){
    val x : MethodHandles.Lookup = null!!

    short.<!UNRESOLVED_REFERENCE!>describeConstable<!>()

    long.<!UNRESOLVED_REFERENCE!>describeConstable<!>()
    long.<!UNRESOLVED_REFERENCE!>resolveConstantDesc<!>(x)

    float.<!UNRESOLVED_REFERENCE!>describeConstable<!>()
    float.<!UNRESOLVED_REFERENCE!>resolveConstantDesc<!>(x)

    string.<!UNRESOLVED_REFERENCE!>describeConstable<!>()
    string.<!UNRESOLVED_REFERENCE!>resolveConstantDesc<!>(x)
    string.indexOf(<!ARGUMENT_TYPE_MISMATCH!>1<!>,1,<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    string.indexOf(<!ARGUMENT_TYPE_MISMATCH!>""<!>, 1,<!ARGUMENT_TYPE_MISMATCH!>2<!>)
    string.<!UNRESOLVED_REFERENCE!>splitWithDelimiters<!>("", 1)
    string.<!UNRESOLVED_REFERENCE!>strip<!>()
    string.<!UNRESOLVED_REFERENCE!>stripLeading<!>()
    string.<!UNRESOLVED_REFERENCE!>stripTrailing<!>()
    string.<!UNRESOLVED_REFERENCE!>indent<!>(1)
    string.<!UNRESOLVED_REFERENCE!>stripIndent<!>()
    string.<!UNRESOLVED_REFERENCE!>translateEscapes<!>()
    string.<!UNRESOLVED_REFERENCE!>transform<!>({})
    string.<!UNRESOLVED_REFERENCE!>formatted<!>()

    char.<!UNRESOLVED_REFERENCE!>describeConstable<!>()

    collection.<!UNRESOLVED_REFERENCE!>toArray<!> {_: Int -> arrayOf(1)}

    double.<!UNRESOLVED_REFERENCE!>describeConstable<!>()
    double.<!UNRESOLVED_REFERENCE!>resolveConstantDesc<!>(x)

    boolean.<!UNRESOLVED_REFERENCE!>describeConstable<!>()

    list.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    list.<!UNRESOLVED_REFERENCE!>getLast<!>()

    int.<!UNRESOLVED_REFERENCE!>describeConstable<!>()
    int.<!UNRESOLVED_REFERENCE!>resolveConstantDesc<!>(x)

    byte.<!UNRESOLVED_REFERENCE!>describeConstable<!>()

    enumType.<!UNRESOLVED_REFERENCE!>describeConstable<!>()
}

enum class MyEnum