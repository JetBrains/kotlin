// WITH_RUNTIME
// SUGGESTED_NAMES: i, getN
// PARAM_TYPES: kotlin.String?, kotlin.String, kotlin.CharSequence?, CharSequence
// PARAM_DESCRIPTOR: val property: (kotlin.String..kotlin.String?) defined in test
fun test() {
    val property = System.getProperty("some")
    val n = <selection>property?.length()</selection>
}