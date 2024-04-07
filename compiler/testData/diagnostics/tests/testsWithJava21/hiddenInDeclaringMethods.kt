import java.lang.invoke.MethodHandles

fun test(short: Short, long: Long, float: Float,
         string: String, char: Char, collection: Collection<Any>,
         double: Double, boolean: Boolean, list: List<Any>,
         int: Int, byte: Byte, enumType: MyEnum){
    val x : MethodHandles.Lookup = null!!

    short.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>describeConstable<!>()

    long.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>describeConstable<!>()
    long.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>resolveConstantDesc<!>(x)

    float.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>describeConstable<!>()
    float.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>resolveConstantDesc<!>(x)

    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>describeConstable<!>()
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>resolveConstantDesc<!>(x)
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>indexOf<!>(1,1,1)
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>indexOf<!>("", 1,2)
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>splitWithDelimiters<!>("", 1)
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>strip<!>()
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>stripLeading<!>()
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>stripTrailing<!>()
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>indent<!>(1)
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>stripIndent<!>()
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>translateEscapes<!>()
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>transform<!>({})
    string.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>formatted<!>()

    char.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>describeConstable<!>()

    collection.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>toArray<!> {_: Int -> arrayOf(1)}

    double.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>describeConstable<!>()
    double.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>resolveConstantDesc<!>(x)

    boolean.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>describeConstable<!>()

    list.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    list.<!UNRESOLVED_REFERENCE!>getLast<!>()

    int.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>describeConstable<!>()
    int.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>resolveConstantDesc<!>(x)

    byte.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>describeConstable<!>()

    enumType.<!DEPRECATION!>describeConstable<!>()
}

enum class MyEnum