// ISSUE: KT-64640
// WITH_STDLIB

fun bar(x: List<String>) {
    x.<!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>addFirst<!>("")
    x.<!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>addLast<!>("")
    x.<!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>removeFirst<!>()
    x.<!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>removeLast<!>()
}
