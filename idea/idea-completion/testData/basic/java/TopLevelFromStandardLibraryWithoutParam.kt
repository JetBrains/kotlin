package testing

fun someFun() {
  emptySequen<caret>
}

// Important: This test checks that completion will find top level functions from jars.
// If you going to update it make sure that methods are not auto-imported

// EXIST: emptySequence