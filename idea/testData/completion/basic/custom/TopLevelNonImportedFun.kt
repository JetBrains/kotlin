package testing

fun someFun() {
  abcd<caret>
}

// EXIST: abcdAAA1, abcdBBB2
// ABSENT: abcdCCC3, abcdDDD4
