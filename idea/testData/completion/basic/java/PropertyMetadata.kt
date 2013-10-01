/* Number of result in completion should be fixed after removing duplicates */
package first

fun firstFun() {
  val a = PropertyM<caret>
}

// INVOCATION_COUNT: 1
// EXIST: PropertyMetadata@PropertyMetadata~(jet)
// EXIST: PropertyMetadataImpl@PropertyMetadataImpl~(jet)
// NUMBER: 2
