/* Number of result in completion should be fixed after removing duplicates */
package first

fun firstFun() {
  val a = PropertyM<caret>
}

// INVOCATION_COUNT: 1
// EXIST: { lookupString:"PropertyMetadata", itemText:"PropertyMetadata", tailText:" (jet)" }
// EXIST: { lookupString:"PropertyMetadataImpl", itemText:"PropertyMetadataImpl", tailText:" (jet)" }
// NUMBER: 2
