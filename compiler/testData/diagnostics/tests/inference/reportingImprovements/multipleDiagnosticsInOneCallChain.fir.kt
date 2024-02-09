// ISSUE: KT-66186
// WITH_STDLIB
// FULL_JDK

fun test() {
    val list = listOf("asd", "sda")
    list.stream()
        .filter(String::isNotEmpty)
        .<!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>collect<!>(<!UNRESOLVED_REFERENCE!>FakeClass<!>.toList())
}
