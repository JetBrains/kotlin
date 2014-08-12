package testing

fun someFun() {
  "".hello<caret>
}

// EXIST: helloProp1, helloProp2
// ABSENT: helloProp3, helloProp4
