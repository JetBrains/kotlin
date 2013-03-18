package testing

fun someFun() {
  1.abcd<caret>
}

// EXIST: abcdCCC3, abcdDDD4
// ABSENT: abcdAAA1, abcdBBB2
